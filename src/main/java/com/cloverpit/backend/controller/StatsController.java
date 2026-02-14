package com.cloverpit.backend.controller;

import com.cloverpit.backend.dto.ClanStatsResponse;
import com.cloverpit.backend.model.Player;
import com.cloverpit.backend.service.PlayerService;
import com.cloverpit.backend.service.PubgService;
import com.cloverpit.backend.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;
    private final PubgService pubgService;
    private final PlayerService playerService;

    @GetMapping("/clan")
    public ResponseEntity<ClanStatsResponse> getClanStats() {
        return ResponseEntity.ok(statsService.getClanStats());
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refreshStats() {
        pubgService.refreshAllPlayersStats();
        return ResponseEntity.ok("Stats refreshed successfully");
    }

    @PostMapping("/refresh/{playerId}")
    public ResponseEntity<String> refreshPlayerStats(@PathVariable String playerId) {
        Player player = playerService.getPlayerById(playerId);
        if (player == null) {
            return ResponseEntity.notFound().build();
        }
        pubgService.refreshPlayerStats(player);
        return ResponseEntity.ok("Stats refreshed for player: " + player.getPubgName());
    }
}
