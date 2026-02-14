package com.cloverpit.backend.service;

import com.cloverpit.backend.dto.ClanStatsResponse;
import com.cloverpit.backend.model.Match;
import com.cloverpit.backend.model.Player;
import com.cloverpit.backend.repository.MatchRepository;
import com.cloverpit.backend.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;

    public ClanStatsResponse getClanStats() {
        List<Player> players = playerRepository.findAll();
        
        if (players.isEmpty()) {
            return ClanStatsResponse.builder()
                    .totalMembers(0)
                    .avgKD(0.0)
                    .avgDamage(0.0)
                    .totalMatches(0)
                    .build();
        }

        int totalMembers = players.size();
        double avgKD = players.stream()
                .mapToDouble(p -> p.getStats().getKd())
                .average()
                .orElse(0.0);
        double avgDamage = players.stream()
                .mapToDouble(p -> p.getStats().getAverageDamage())
                .average()
                .orElse(0.0);
        
        // 전체 매치에서 고유한 matchId만 계산 (같은 매치에 여러 클랜원이 참여한 경우 중복 제거)
        List<Match> allMatches = matchRepository.findAll();
        Set<String> uniqueMatchIds = new HashSet<>();
        for (Match match : allMatches) {
            if (match.getMatchId() != null) {
                uniqueMatchIds.add(match.getMatchId());
            }
        }
        int totalMatches = uniqueMatchIds.size();
        
        log.debug("Total unique matches: {} (from {} match records)", totalMatches, allMatches.size());

        return ClanStatsResponse.builder()
                .totalMembers(totalMembers)
                .avgKD(Math.round(avgKD * 100.0) / 100.0)
                .avgDamage(Math.round(avgDamage * 100.0) / 100.0)
                .totalMatches(totalMatches)
                .build();
    }

    public void calculateAndUpdateScore(Player player) {
        Player.PlayerStats stats = player.getStats();
        
        // 경쟁전 위주 점수 계산 (경쟁전 가중치 70%, 전체 30%)
        // score = (경쟁전 KD × 0.4) + (경쟁전 평균 데미지 / 100 × 0.3) + (경쟁전 매치 수 로그 × 0.2) + (승률 × 0.1)
        
        double rankedKdScore = stats.getRankedKd() * 0.4;
        double rankedDamageScore = (stats.getRankedAvgDamage() / 100.0) * 0.3;
        double rankedMatchScore = stats.getRankedMatches() > 0 ? 
                Math.log(stats.getRankedMatches() + 1) * 0.2 : 0.0;
        
        // 승률 계산
        double winRate = stats.getTotalMatches() > 0 ? 
                (double) stats.getWins() / stats.getTotalMatches() : 0.0;
        double winScore = winRate * 0.1;
        
        double totalScore = rankedKdScore + rankedDamageScore + rankedMatchScore + winScore;
        
        // 전체 통계로 보정 (경쟁전이 너무 적은 경우)
        if (stats.getRankedMatches() < 10) {
            double generalKdScore = stats.getKd() * 0.3;
            double generalDamageScore = (stats.getAverageDamage() / 100.0) * 0.2;
            totalScore = (totalScore + generalKdScore + generalDamageScore) * 0.7;
        }
        
        player.setScore(Math.round(totalScore * 10.0) / 10.0);
        
        playerRepository.save(player);
        log.debug("Updated score for player {}: {} (ranked: {}, total: {})", 
                player.getPubgName(), player.getScore(), stats.getRankedMatches(), stats.getTotalMatches());
    }

    public void recalculateAllScores() {
        List<Player> players = playerRepository.findAll();
        players.forEach(this::calculateAndUpdateScore);
        log.info("Recalculated scores for {} players", players.size());
    }
}
