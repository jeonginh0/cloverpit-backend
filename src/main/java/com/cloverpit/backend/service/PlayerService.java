package com.cloverpit.backend.service;

import com.cloverpit.backend.dto.CreatePlayerRequest;
import com.cloverpit.backend.model.Player;
import com.cloverpit.backend.repository.MatchRepository;
import com.cloverpit.backend.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;
    private final StatsService statsService;

    public List<Player> getRankings(String sortBy, String sortOrder) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder) ? 
                Sort.Direction.ASC : Sort.Direction.DESC;
        
        String sortField = switch (sortBy) {
            case "kd" -> "stats.kd";
            case "averageDamage" -> "stats.averageDamage";
            case "totalMatches" -> "stats.totalMatches";
            default -> "score";
        };
        
        return playerRepository.findAll(Sort.by(direction, sortField));
    }

    public Player getPlayerById(String id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Player not found"));
    }

    public Player createPlayer(CreatePlayerRequest request) {
        if (playerRepository.existsByPubgName(request.getPubgName())) {
            throw new RuntimeException("Player already exists");
        }

        Player player = Player.builder()
                .pubgName(request.getPubgName())
                .discordName(request.getDiscordName())
                .stats(new Player.PlayerStats())
                .score(0.0)
                .build();

        return playerRepository.save(player);
    }

    public void deletePlayer(String id) {
        log.info("Deleting player with id: {}", id);
        
        // 해당 플레이어의 모든 매치 삭제
        matchRepository.deleteByPlayerId(id);
        log.info("Deleted all matches for player: {}", id);
        
        // 플레이어 삭제
        playerRepository.deleteById(id);
        log.info("Deleted player: {}", id);
    }

    public void updatePlayerStats(Player player) {
        playerRepository.save(player);
        statsService.calculateAndUpdateScore(player);
    }
}
