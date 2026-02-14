package com.cloverpit.backend.service;

import com.cloverpit.backend.model.Match;
import com.cloverpit.backend.model.Player;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class PubgService {

    private final PlayerService playerService;
    private final MatchService matchService;
    private final StatsService statsService;

    @Value("${pubg.api-key:}")
    private String apiKey;

    @Value("${pubg.base-url:https://api.pubg.com/shards}")
    private String baseUrl;

    @Value("${pubg.platform:steam}")
    private String platform;

    @Value("${pubg.shard:kakao}")
    private String shard;

    private WebClient webClient;

    /**
     * PUBG API를 사용하여 플레이어 전적을 갱신합니다.
     * API 키가 없는 경우 더미 데이터를 생성합니다.
     */
    public void refreshPlayerStats(Player player) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.error("PUBG API key not configured. Cannot refresh stats for: {}", player.getPubgName());
            return; // 에러를 던지지 않고 그냥 리턴
        }

        try {
            // WebClient 초기화
            if (webClient == null) {
                webClient = WebClient.builder()
                        .baseUrl(baseUrl)
                        .defaultHeader("Authorization", "Bearer " + apiKey)
                        .defaultHeader("Accept", "application/vnd.api+json")
                        .build();
            }

            log.info("Fetching PUBG data for player: {}", player.getPubgName());
            
            // 1. 플레이어 계정 정보 조회
            String accountId = getPlayerAccountId(player.getPubgName());
            if (accountId == null) {
                log.warn("Player not found in PUBG API: {} - keeping existing data", player.getPubgName());
                // 기존 데이터가 있으면 통계만 재계산
                List<Match> existingMatches = matchService.getPlayerMatches(player.getId());
                if (!existingMatches.isEmpty()) {
                    log.info("Recalculating stats from {} existing matches", existingMatches.size());
                    updatePlayerStatistics(player);
                } else {
                    log.warn("No existing matches for player: {}", player.getPubgName());
                }
                return; // 에러를 던지지 않고 그냥 리턴
            }

            // 2. 현재 시즌 ID 조회
            String currentSeasonId = getCurrentSeasonId();
            if (currentSeasonId == null) {
                log.warn("Could not fetch current season, using recent matches only");
            } else {
                log.info("Current season: {}", currentSeasonId);
            }

            // 3. 최근 매치 리스트 조회 (최근 14일)
            List<String> matchIds = getPlayerMatchIds(accountId);
            if (matchIds.isEmpty()) {
                log.warn("No recent matches found for player: {}", player.getPubgName());
                // 기존 데이터가 있으면 통계만 재계산
                List<Match> existingMatches = matchService.getPlayerMatches(player.getId());
                if (!existingMatches.isEmpty()) {
                    log.info("Recalculating stats from {} existing matches", existingMatches.size());
                    updatePlayerStatistics(player);
                } else {
                    log.warn("No existing matches and no recent matches found for: {}", player.getPubgName());
                }
                return;
            }

            log.info("Found {} recent matches for player: {}", matchIds.size(), player.getPubgName());

            // 4. 기존 매치 확인
            List<Match> existingMatches = matchService.getPlayerMatches(player.getId());
            List<String> existingMatchIds = existingMatches.stream()
                    .map(Match::getMatchId)
                    .toList();

            // 5. 각 매치 상세 정보 조회 및 저장
            int newMatchCount = 0;
            for (String matchId : matchIds) {
                if (existingMatchIds.contains(matchId)) {
                    continue; // 이미 저장된 매치는 스킵
                }

                try {
                    Match match = getMatchDetails(matchId, player, currentSeasonId);
                    if (match != null) {
                        matchService.createMatch(match);
                        newMatchCount++;
                    }
                    
                    // API 요청 제한 준수 (10 requests/min)
                    Thread.sleep(100);
                } catch (Exception e) {
                    log.error("Failed to fetch match details: " + matchId, e);
                }
            }

            log.info("Added {} new matches for player: {}", newMatchCount, player.getPubgName());

            // 6. 통계 재계산
            updatePlayerStatistics(player);
            
        } catch (Exception e) {
            log.error("Failed to fetch PUBG stats for: {} - Error: {}", player.getPubgName(), e.getMessage());
            // 기존 데이터가 있으면 통계만 재계산
            try {
                List<Match> existingMatches = matchService.getPlayerMatches(player.getId());
                if (!existingMatches.isEmpty()) {
                    log.info("Recalculating stats from {} existing matches after error", existingMatches.size());
                    updatePlayerStatistics(player);
                }
            } catch (Exception statEx) {
                log.error("Failed to recalculate stats for: {}", player.getPubgName(), statEx);
            }
            // 에러를 던지지 않음 - 다른 플레이어 갱신을 계속 진행
        }
    }

    /**
     * 전체 플레이어의 전적을 갱신합니다.
     * Rate Limit을 피하기 위해 배치 단위로 처리합니다.
     */
    public void refreshAllPlayersStats() {
        List<Player> players = playerService.getRankings("totalMatches", "desc");
        log.info("Refreshing stats for {} players", players.size());

        int batchSize = 3; // 3명씩 배치 처리
        int batchDelayMs = 10000; // 배치 사이 10초 대기
        int totalBatches = (int) Math.ceil((double) players.size() / batchSize);
        
        for (int i = 0; i < players.size(); i += batchSize) {
            int currentBatch = (i / batchSize) + 1;
            int endIndex = Math.min(i + batchSize, players.size());
            List<Player> batch = players.subList(i, endIndex);
            
            log.info("Processing batch {}/{} ({} players)", currentBatch, totalBatches, batch.size());
            
            for (Player player : batch) {
                try {
                    refreshPlayerStats(player);
                } catch (Exception e) {
                    log.error("Failed to refresh stats for player: " + player.getPubgName(), e);
                }
            }
            
            // 마지막 배치가 아니면 대기
            if (endIndex < players.size()) {
                log.info("Batch {} completed. Waiting {}ms before next batch...", currentBatch, batchDelayMs);
                try {
                    Thread.sleep(batchDelayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Batch delay interrupted", e);
                    break;
                }
            }
        }

        log.info("All batches completed. Recalculating scores...");
        statsService.recalculateAllScores();
        log.info("Stats refresh completed for all players");
    }

    /**
     * 현재 시즌 ID 조회
     */
    private String getCurrentSeasonId() {
        try {
            log.debug("Fetching current season for shard: {}", shard);
            JsonNode response = webClient.get()
                    .uri("/{shard}/seasons", shard)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response != null && response.has("data") && response.get("data").isArray()) {
                for (JsonNode season : response.get("data")) {
                    JsonNode attributes = season.get("attributes");
                    if (attributes != null && attributes.has("isCurrentSeason") && 
                        attributes.get("isCurrentSeason").asBoolean()) {
                        String seasonId = season.get("id").asText();
                        log.info("Current season ID: {}", seasonId);
                        return seasonId;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch current season: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * PUBG API에서 플레이어 계정 ID 조회
     * Rate limit 에러(429) 발생 시 재시도 로직 포함
     */
    private String getPlayerAccountId(String playerName) {
        int maxRetries = 3;
        int retryDelayMs = 5000; // 5초
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.debug("Fetching account ID for player: {} (attempt {}/{})", playerName, attempt, maxRetries);
                JsonNode response = webClient.get()
                        .uri("/{shard}/players?filter[playerNames]={playerName}", shard, playerName)
                        .retrieve()
                        .onStatus(
                            status -> status.value() == 429,
                            clientResponse -> {
                                log.warn("Rate limit hit (429) for player: {} - will retry", playerName);
                                return Mono.error(new RuntimeException("RATE_LIMIT"));
                            }
                        )
                        .onStatus(
                            status -> status.is4xxClientError(),
                            clientResponse -> {
                                log.warn("Client error {} for player: {}", clientResponse.statusCode(), playerName);
                                return Mono.error(new RuntimeException("CLIENT_ERROR: " + clientResponse.statusCode()));
                            }
                        )
                        .onStatus(
                            status -> status.is5xxServerError(),
                            clientResponse -> {
                                log.error("Server error {} from PUBG API", clientResponse.statusCode());
                                return Mono.error(new RuntimeException("SERVER_ERROR: " + clientResponse.statusCode()));
                            }
                        )
                        .bodyToMono(JsonNode.class)
                        .block();

                if (response != null && response.has("data") && response.get("data").isArray() && response.get("data").size() > 0) {
                    String accountId = response.get("data").get(0).get("id").asText();
                    log.debug("Found account ID: {} for player: {}", accountId, playerName);
                    return accountId;
                }
                log.warn("No account found in API response for player: {}", playerName);
                return null; // 플레이어를 찾지 못한 경우 재시도 불필요
                
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                
                // Rate limit 에러인 경우 재시도
                if (errorMsg != null && errorMsg.contains("RATE_LIMIT")) {
                    if (attempt < maxRetries) {
                        log.warn("Rate limit hit for player: {} - retrying in {}ms (attempt {}/{})", 
                                playerName, retryDelayMs, attempt, maxRetries);
                        try {
                            Thread.sleep(retryDelayMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.error("Retry sleep interrupted");
                            return null;
                        }
                        continue; // 다음 시도
                    } else {
                        log.error("Rate limit exceeded after {} attempts for player: {}", maxRetries, playerName);
                        return null;
                    }
                }
                
                // 다른 에러는 재시도하지 않음
                log.error("Failed to fetch player account ID for: {} - Error: {}", playerName, errorMsg);
                return null;
            }
        }
        
        return null;
    }

    /**
     * 플레이어의 최근 매치 ID 리스트 조회
     */
    private List<String> getPlayerMatchIds(String accountId) {
        List<String> matchIds = new ArrayList<>();
        try {
            JsonNode response = webClient.get()
                    .uri("/{shard}/players/{accountId}", shard, accountId)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response != null && response.has("data")) {
                JsonNode relationships = response.get("data").get("relationships");
                if (relationships != null && relationships.has("matches")) {
                    JsonNode matches = relationships.get("matches").get("data");
                    if (matches.isArray()) {
                        for (JsonNode match : matches) {
                            matchIds.add(match.get("id").asText());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch match IDs for account: " + accountId, e);
        }
        return matchIds;
    }

    /**
     * 매치 상세 정보 조회 및 Match 객체 생성
     */
    private Match getMatchDetails(String matchId, Player player) {
        return getMatchDetails(matchId, player, null);
    }
    
    /**
     * 매치 상세 정보 조회 및 Match 객체 생성 (시즌 ID 포함)
     */
    private Match getMatchDetails(String matchId, Player player, String seasonId) {
        try {
            log.debug("Fetching match details for: {}", matchId);
            JsonNode response = webClient.get()
                    .uri("/{shard}/matches/{matchId}", shard, matchId)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null || !response.has("data")) {
                return null;
            }

            JsonNode data = response.get("data");
            JsonNode attributes = data.get("attributes");
            
            // 매치 기본 정보
            String gameMode = attributes.get("gameMode").asText();
            String matchType = attributes.get("matchType").asText();
            String createdAtStr = attributes.get("createdAt").asText();
            LocalDateTime createdAt = ZonedDateTime.parse(createdAtStr, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime();
            
            // seasonId가 API 응답에 있으면 사용
            if (seasonId == null && attributes.has("seasonState")) {
                seasonId = attributes.get("seasonState").asText();
            }

            // gameMode 정규화 (solo, duo, squad)
            String normalizedGameMode = normalizeGameMode(gameMode);
            String normalizedMatchType = normalizeMatchType(matchType);

            // 참가자 정보에서 플레이어 통계 찾기
            JsonNode included = response.get("included");
            if (included == null || !included.isArray()) {
                return null;
            }

            // 1단계: 플레이어의 roster ID 찾기
            String playerRosterId = null;
            String playerParticipantId = null;
            
            for (JsonNode item : included) {
                if ("participant".equals(item.get("type").asText())) {
                    JsonNode participantAttrs = item.get("attributes");
                    JsonNode stats = participantAttrs.get("stats");
                    String participantName = stats.get("name").asText();
                    
                    if (participantName.equalsIgnoreCase(player.getPubgName())) {
                        playerParticipantId = item.get("id").asText();
                        break;
                    }
                }
            }
            
            // 플레이어를 찾지 못한 경우
            if (playerParticipantId == null) {
                log.warn("Player {} not found in match {}", player.getPubgName(), matchId);
                return null;
            }
            
            // 2단계: roster에서 플레이어의 팀 찾기
            for (JsonNode item : included) {
                if ("roster".equals(item.get("type").asText())) {
                    JsonNode relationships = item.get("relationships");
                    if (relationships != null && relationships.has("participants")) {
                        JsonNode participantsData = relationships.get("participants").get("data");
                        if (participantsData.isArray()) {
                            for (JsonNode participantRef : participantsData) {
                                if (playerParticipantId.equals(participantRef.get("id").asText())) {
                                    playerRosterId = item.get("id").asText();
                                    break;
                                }
                            }
                        }
                    }
                    if (playerRosterId != null) break;
                }
            }
            
            // 3단계: 같은 roster의 모든 participant ID 수집
            List<String> teamParticipantIds = new ArrayList<>();
            if (playerRosterId != null) {
                for (JsonNode item : included) {
                    if ("roster".equals(item.get("type").asText()) && 
                        playerRosterId.equals(item.get("id").asText())) {
                        JsonNode relationships = item.get("relationships");
                        if (relationships != null && relationships.has("participants")) {
                            JsonNode participantsData = relationships.get("participants").get("data");
                            if (participantsData.isArray()) {
                                for (JsonNode participantRef : participantsData) {
                                    teamParticipantIds.add(participantRef.get("id").asText());
                                }
                            }
                        }
                        break;
                    }
                }
            }
            
            // 4단계: 팀원들의 이름 수집 및 플레이어 통계 찾기
            List<String> teamMates = new ArrayList<>();
            Map<String, Integer> weaponKills = new HashMap<>();
            Match playerMatch = null;

            for (JsonNode item : included) {
                if ("participant".equals(item.get("type").asText())) {
                    String participantId = item.get("id").asText();
                    
                    // 같은 팀원인지 확인
                    if (teamParticipantIds.contains(participantId)) {
                        JsonNode participantAttrs = item.get("attributes");
                        JsonNode stats = participantAttrs.get("stats");
                        String participantName = stats.get("name").asText();
                        
                        teamMates.add(participantName);
                        
                        // 현재 플레이어의 통계 저장
                        if (participantName.equalsIgnoreCase(player.getPubgName())) {
                            playerMatch = Match.builder()
                                    .playerId(player.getId())
                                    .playerName(player.getPubgName())
                                    .matchId(matchId)
                                    .seasonId(seasonId)
                                    .gameMode(normalizedGameMode)
                                    .matchType(normalizedMatchType)
                                    .kills(stats.get("kills").asInt())
                                    .deaths(stats.get("deathType").asText().equals("alive") ? 0 : 1)
                                    .damage(stats.get("damageDealt").asDouble())
                                    .placement(stats.get("winPlace").asInt())
                                    .timeSurvived(stats.get("timeSurvived").asInt())
                                    .participants(teamMates)
                                    .createdAt(createdAt)
                                    .build();
                        }
                    }
                }
            }
            
            return playerMatch;
        } catch (Exception e) {
            log.error("Failed to parse match details for matchId: " + matchId, e);
        }
        return null;
    }

    /**
     * 무기 이름 정규화 (PUBG 내부 이름을 사용자 친화적인 이름으로)
     */
    private String normalizeWeaponName(String rawName) {
        if (rawName == null || rawName.isEmpty()) {
            return "Unknown";
        }
        
        // PUBG 무기 내부 이름 패턴 처리
        String normalized = rawName;
        
        // WeaponName_C 형식 처리
        if (normalized.endsWith("_C")) {
            normalized = normalized.substring(0, normalized.length() - 2);
        }
        
        // Weapon 접두사 제거
        if (normalized.startsWith("Weapon")) {
            normalized = normalized.substring(6);
        }
        
        // Item 접두사 제거
        if (normalized.startsWith("Item_")) {
            normalized = normalized.substring(5);
        }
        
        // 언더스코어를 공백으로
        normalized = normalized.replace("_", " ");
        
        // 특수 케이스 처리
        normalized = switch (normalized.toLowerCase()) {
            case "akm" -> "AKM";
            case "m416" -> "M416";
            case "scar-l", "scarl" -> "SCAR-L";
            case "m16a4" -> "M16A4";
            case "aug" -> "AUG";
            case "groza" -> "Groza";
            case "kar98k" -> "Kar98k";
            case "m24" -> "M24";
            case "awm" -> "AWM";
            case "mini14" -> "Mini14";
            case "sks" -> "SKS";
            case "vss" -> "VSS";
            case "slr" -> "SLR";
            case "mk14" -> "Mk14";
            case "ump45", "ump" -> "UMP45";
            case "vector" -> "Vector";
            case "uzi" -> "Uzi";
            case "tommy gun", "tommygun" -> "Tommy Gun";
            case "s12k" -> "S12K";
            case "s1897" -> "S1897";
            case "s686" -> "S686";
            case "dbs" -> "DBS";
            case "crossbow" -> "Crossbow";
            case "grenade", "frag grenade" -> "Grenade";
            case "molotov" -> "Molotov";
            case "smoke grenade" -> "Smoke";
            case "stun grenade" -> "Stun";
            case "melee", "punch", "pan", "crowbar", "sickle", "machete" -> "Melee";
            case "vehicle", "car", "buggy", "dacia", "uaz", "motorcycle" -> "Vehicle";
            case "bluezone", "blue zone" -> "Blue Zone";
            case "redzone", "red zone", "bomb", "bombing" -> "Red Zone";
            default -> normalized.trim();
        };
        
        return normalized;
    }

    /**
     * 게임 모드 정규화 (solo, duo, squad)
     */
    private String normalizeGameMode(String gameMode) {
        String lower = gameMode.toLowerCase();
        log.debug("Normalizing game mode: {} -> {}", gameMode, lower);
        
        if (lower.contains("solo")) return "solo";
        if (lower.contains("duo")) return "duo";
        if (lower.contains("squad") || lower.contains("fpp") || lower.contains("tpp")) return "squad";
        
        // 기본값: squad (1인 모드가 아니면 대부분 squad)
        log.warn("Unknown game mode: {}, defaulting to squad", gameMode);
        return "squad";
    }

    /**
     * 매치 타입 정규화 (ranked, normal)
     * PUBG API matchType 값:
     * - "competitive" = 경쟁전 (ranked)
     * - "official" = 일반전 (normal)
     * - "custom" = 커스텀 (일반적으로 제외)
     * - "event" = 이벤트 (일반적으로 제외)
     */
    private String normalizeMatchType(String matchType) {
        String lower = matchType.toLowerCase();
        log.debug("Normalizing match type: {} -> {}", matchType, lower);
        
        // 경쟁전 판별
        if (lower.contains("competitive") || lower.contains("ranked")) {
            return "ranked";
        }
        
        // 일반 공식 매치
        if (lower.contains("official") || lower.contains("normal")) {
            return "normal";
        }
        
        // 커스텀이나 이벤트는 제외하고 싶을 수 있지만, 일단 normal로 분류
        log.debug("Match type '{}' classified as normal", matchType);
        return "normal";
    }

    /**
     * 플레이어 통계 재계산 및 업데이트 (현재 시즌만)
     */
    private void updatePlayerStatistics(Player player) {
        // 현재 시즌 ID 조회
        String currentSeasonId = getCurrentSeasonId();
        
        // 플레이어의 전체 매치 가져오기
        List<Match> allMatches = matchService.getPlayerMatches(player.getId());
        
        // 현재 시즌 매치만 필터링
        if (currentSeasonId != null) {
            allMatches = allMatches.stream()
                    .filter(m -> currentSeasonId.equals(m.getSeasonId()) || m.getSeasonId() == null)
                    .toList();
            log.info("Filtering matches for season {}: {} matches", currentSeasonId, allMatches.size());
        }
        
        // 경쟁전 매치 필터링
        List<Match> rankedMatches = allMatches.stream()
                .filter(m -> "ranked".equals(m.getMatchType()))
                .toList();
        
        // 일반전 매치 필터링
        List<Match> normalMatches = allMatches.stream()
                .filter(m -> "normal".equals(m.getMatchType()))
                .toList();
        
        // 전체 시즌 통계 계산
        Player.PlayerStats stats = new Player.PlayerStats();
        
        // 전체 통계
        stats.setTotalMatches(allMatches.size());
        stats.setKills(allMatches.stream().mapToInt(Match::getKills).sum());
        stats.setDeaths(allMatches.stream().mapToInt(Match::getDeaths).sum());
        stats.setKd(calculateKD(stats.getKills(), stats.getDeaths()));
        stats.setAverageDamage(calculateAvgDamage(allMatches));
        stats.setWins((int) allMatches.stream().filter(m -> m.getPlacement() == 1).count());
        stats.setTop10((int) allMatches.stream().filter(m -> m.getPlacement() <= 10).count());
        
        // 경쟁전 통계 (평균 데미지는 스쿼드만)
        List<Match> rankedSquadMatches = rankedMatches.stream()
                .filter(m -> "squad".equals(m.getGameMode()))
                .toList();
        stats.setRankedMatches(rankedMatches.size());
        int rankedKills = rankedMatches.stream().mapToInt(Match::getKills).sum();
        int rankedDeaths = rankedMatches.stream().mapToInt(Match::getDeaths).sum();
        stats.setRankedKd(calculateKD(rankedKills, rankedDeaths));
        stats.setRankedAvgDamage(calculateAvgDamage(rankedSquadMatches));
        stats.setRankedWins((int) rankedMatches.stream().filter(m -> m.getPlacement() == 1).count());
        
        // 일반전 통계 (평균 데미지는 스쿼드만)
        List<Match> normalSquadMatches = normalMatches.stream()
                .filter(m -> "squad".equals(m.getGameMode()))
                .toList();
        stats.setNormalMatches(normalMatches.size());
        int normalKills = normalMatches.stream().mapToInt(Match::getKills).sum();
        int normalDeaths = normalMatches.stream().mapToInt(Match::getDeaths).sum();
        stats.setNormalKd(calculateKD(normalKills, normalDeaths));
        stats.setNormalAvgDamage(calculateAvgDamage(normalSquadMatches));
        stats.setNormalWins((int) normalMatches.stream().filter(m -> m.getPlacement() == 1).count());
        
        // 모드별 통계
        stats.setSoloMatches((int) allMatches.stream().filter(m -> "solo".equals(m.getGameMode())).count());
        stats.setDuoMatches((int) allMatches.stream().filter(m -> "duo".equals(m.getGameMode())).count());
        stats.setSquadMatches((int) allMatches.stream().filter(m -> "squad".equals(m.getGameMode())).count());
        
        player.setStats(stats);
        playerService.updatePlayerStats(player);
        
        log.info("Stats updated for {}: {} total (Ranked: {}, Normal: {}), K/D: {}, Ranked K/D: {}", 
                player.getPubgName(), stats.getTotalMatches(), stats.getRankedMatches(), 
                stats.getNormalMatches(), stats.getKd(), stats.getRankedKd());
    }

    /**
     * 테스트용 더미 데이터 생성
     * 실제 PUBG API처럼 시즌 전체 매치 데이터를 시뮬레이션
     * 
     * 주의: 이 메서드는 실제 PUBG API 연동 전까지 임시로 사용됩니다.
     * 플레이어별로 선호 모드를 설정하여 더 현실적인 데이터를 생성합니다.
     */
    private void generateDummyStats(Player player) {
        Random random = new Random(player.getId().hashCode()); // 플레이어별 시드 고정
        
        // 기존 매치 확인
        List<Match> existingMatches = matchService.getPlayerMatches(player.getId());
        
        int matchesToGenerate;
        if (existingMatches.isEmpty()) {
            // 첫 갱신: 시즌 전체 매치 생성 (50-100개)
            matchesToGenerate = 50 + random.nextInt(51);
            log.info("First refresh for {}: generating {} season matches", 
                    player.getPubgName(), matchesToGenerate);
        } else {
            // 이후 갱신: 최근 매치만 추가 (5-10개)
            matchesToGenerate = 5 + random.nextInt(6);
            log.info("Incremental refresh for {}: generating {} new matches", 
                    player.getPubgName(), matchesToGenerate);
        }
        
        // 플레이어별 선호 모드 결정 (일관성 있게)
        String[] gameModes = {"solo", "duo", "squad"};
        String preferredMode = gameModes[Math.abs(player.getPubgName().hashCode()) % 3];
        
        // 선호 모드 70%, 다른 모드 30% 분배
        int preferredModeWeight = 70;
        
        // 경쟁전/일반전 비율 (경쟁전 80%, 일반전 20% - 더 현실적으로)
        int rankedWeight = 80;
        
        // 현재 시즌 ID (더미 데이터용)
        String dummySeasonId = "division.bro.official.pc-2018-01"; // 더미 시즌 ID
        
        log.info("Player {} preferred mode: {}, ranked weight: {}%", player.getPubgName(), preferredMode, rankedWeight);
        
        // 새로운 매치 데이터 생성
        for (int i = 0; i < matchesToGenerate; i++) {
            // 게임 모드 결정 (선호도 반영)
            String gameMode;
            int modeRoll = random.nextInt(100);
            if (modeRoll < preferredModeWeight) {
                gameMode = preferredMode;
            } else {
                // 나머지 두 모드 중 랜덤 선택
                String[] otherModes = new String[2];
                int idx = 0;
                for (String mode : gameModes) {
                    if (!mode.equals(preferredMode)) {
                        otherModes[idx++] = mode;
                    }
                }
                gameMode = otherModes[random.nextInt(2)];
            }
            
            // 매치 타입 결정 (경쟁전 선호)
            String matchType = random.nextInt(100) < rankedWeight ? "ranked" : "normal";
            
            int kills = random.nextInt(8);
            int deaths = random.nextBoolean() ? 1 : 0;
            double damage = 100 + random.nextDouble() * 400;
            
            // 경쟁전이 더 어려움
            int placement = matchType.equals("ranked") ? 
                    10 + random.nextInt(80) : 1 + random.nextInt(100);
            int timeSurvived = 300 + random.nextInt(1500);
            
            // 첫 갱신인 경우 과거 날짜로 분산
            LocalDateTime matchDate = existingMatches.isEmpty() 
                    ? LocalDateTime.now().minusDays(random.nextInt(90)) // 최근 3개월
                    : LocalDateTime.now().minusDays(i); // 최근 며칠
            
            Match match = Match.builder()
                    .playerId(player.getId())
                    .playerName(player.getPubgName())
                    .seasonId(dummySeasonId)
                    .gameMode(gameMode)
                    .matchType(matchType)
                    .kills(kills)
                    .deaths(deaths)
                    .damage(damage)
                    .placement(placement)
                    .timeSurvived(timeSurvived)
                    .matchId("match_" + System.currentTimeMillis() + "_" + random.nextInt(10000))
                    .createdAt(matchDate)
                    .build();
            
            matchService.createMatch(match);
            
            // 매치 생성 간 최소 딜레이 (동일 타임스탬프 방지)
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // 플레이어의 전체 매치 가져오기 (시즌 전체)
        List<Match> allMatches = matchService.getPlayerMatches(player.getId());
        
        // 경쟁전 매치 필터링
        List<Match> rankedMatches = allMatches.stream()
                .filter(m -> "ranked".equals(m.getMatchType()))
                .toList();
        
        // 일반전 매치 필터링
        List<Match> normalMatches = allMatches.stream()
                .filter(m -> "normal".equals(m.getMatchType()))
                .toList();
        
        // 전체 시즌 통계 계산
        Player.PlayerStats stats = new Player.PlayerStats();
        
        // 전체 통계
        stats.setTotalMatches(allMatches.size());
        stats.setKills(allMatches.stream().mapToInt(Match::getKills).sum());
        stats.setDeaths(allMatches.stream().mapToInt(Match::getDeaths).sum());
        stats.setKd(calculateKD(stats.getKills(), stats.getDeaths()));
        stats.setAverageDamage(calculateAvgDamage(allMatches));
        stats.setWins((int) allMatches.stream().filter(m -> m.getPlacement() == 1).count());
        stats.setTop10((int) allMatches.stream().filter(m -> m.getPlacement() <= 10).count());
        
        // 경쟁전 통계 (평균 데미지는 스쿼드만)
        List<Match> rankedSquadMatches = rankedMatches.stream()
                .filter(m -> "squad".equals(m.getGameMode()))
                .toList();
        stats.setRankedMatches(rankedMatches.size());
        int rankedKills = rankedMatches.stream().mapToInt(Match::getKills).sum();
        int rankedDeaths = rankedMatches.stream().mapToInt(Match::getDeaths).sum();
        stats.setRankedKd(calculateKD(rankedKills, rankedDeaths));
        stats.setRankedAvgDamage(calculateAvgDamage(rankedSquadMatches));
        stats.setRankedWins((int) rankedMatches.stream().filter(m -> m.getPlacement() == 1).count());
        
        // 일반전 통계 (평균 데미지는 스쿼드만)
        List<Match> normalSquadMatches = normalMatches.stream()
                .filter(m -> "squad".equals(m.getGameMode()))
                .toList();
        stats.setNormalMatches(normalMatches.size());
        int normalKills = normalMatches.stream().mapToInt(Match::getKills).sum();
        int normalDeaths = normalMatches.stream().mapToInt(Match::getDeaths).sum();
        stats.setNormalKd(calculateKD(normalKills, normalDeaths));
        stats.setNormalAvgDamage(calculateAvgDamage(normalSquadMatches));
        stats.setNormalWins((int) normalMatches.stream().filter(m -> m.getPlacement() == 1).count());
        
        // 모드별 통계
        stats.setSoloMatches((int) allMatches.stream().filter(m -> "solo".equals(m.getGameMode())).count());
        stats.setDuoMatches((int) allMatches.stream().filter(m -> "duo".equals(m.getGameMode())).count());
        stats.setSquadMatches((int) allMatches.stream().filter(m -> "squad".equals(m.getGameMode())).count());
        
        player.setStats(stats);
        playerService.updatePlayerStats(player);
        
        log.info("Stats updated for {}: {} total (Ranked: {}, Normal: {}), K/D: {}, Ranked K/D: {}", 
                player.getPubgName(), stats.getTotalMatches(), stats.getRankedMatches(), 
                stats.getNormalMatches(), stats.getKd(), stats.getRankedKd());
    }
    
    private double calculateKD(int kills, int deaths) {
        double kd = deaths > 0 ? (double) kills / deaths : kills;
        return Math.round(kd * 100.0) / 100.0;
    }
    
    private double calculateAvgDamage(List<Match> matches) {
        double avg = matches.stream()
                .mapToDouble(Match::getDamage)
                .average()
                .orElse(0.0);
        return Math.round(avg * 100.0) / 100.0;
    }
}

