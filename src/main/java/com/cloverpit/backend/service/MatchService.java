package com.cloverpit.backend.service;

import com.cloverpit.backend.model.Match;
import com.cloverpit.backend.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;

    public List<Match> getRecentMatches(int limit) {
        // 충분한 양의 매치를 가져와서 중복 제거 후 limit 적용
        Pageable pageable = PageRequest.of(0, limit * 5);
        List<Match> allMatches = matchRepository.findAllByOrderByCreatedAtDesc(pageable);
        
        // matchId로 그룹핑하여 중복 제거
        // 같은 matchId를 가진 매치들 중 대표 매치 하나만 선택하고, 
        // playerNames에 해당 매치에 참가한 모든 클랜원 이름 추가
        Map<String, List<Match>> groupedByMatchId = allMatches.stream()
                .collect(Collectors.groupingBy(Match::getMatchId, LinkedHashMap::new, Collectors.toList()));
        
        List<Match> uniqueMatches = groupedByMatchId.values().stream()
                .map(matchGroup -> {
                    // 첫 번째 매치를 대표로 선택
                    Match representative = matchGroup.get(0);
                    
                    // 해당 매치에 참가한 클랜원들의 이름 수집
                    List<String> clanMembers = matchGroup.stream()
                            .map(Match::getPlayerName)
                            .distinct()
                            .collect(Collectors.toList());
                    
                    // 새로운 Match 객체 생성하여 클랜원 정보 추가
                    Match result = Match.builder()
                            .id(representative.getId())
                            .playerId(null) // 여러 플레이어가 참여한 매치이므로 단일 playerId 제거
                            .playerName(String.join(", ", clanMembers)) // 클랜원들 이름 조합
                            .gameMode(representative.getGameMode())
                            .matchType(representative.getMatchType())
                            .kills(matchGroup.stream().mapToInt(Match::getKills).sum()) // 전체 킬 합산
                            .deaths(matchGroup.stream().mapToInt(Match::getDeaths).sum()) // 전체 데스 합산
                            .damage(matchGroup.stream().mapToDouble(Match::getDamage).average().orElse(0.0)) // 평균 데미지
                            .placement(representative.getPlacement())
                            .timeSurvived(representative.getTimeSurvived())
                            .matchId(representative.getMatchId())
                            .seasonId(representative.getSeasonId())
                            .participants(representative.getParticipants())
                            .createdAt(representative.getCreatedAt())
                            .build();
                    
                    return result;
                })
                .limit(limit)
                .collect(Collectors.toList());
        
        return uniqueMatches;
    }

    public List<Match> getPlayerMatches(String playerId) {
        return matchRepository.findByPlayerIdOrderByCreatedAtDesc(playerId);
    }

    public Match createMatch(Match match) {
        return matchRepository.save(match);
    }
}
