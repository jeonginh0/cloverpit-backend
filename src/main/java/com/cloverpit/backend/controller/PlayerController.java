package com.cloverpit.backend.controller;

import com.cloverpit.backend.dto.CreatePlayerRequest;
import com.cloverpit.backend.model.Player;
import com.cloverpit.backend.service.MatchService;
import com.cloverpit.backend.service.PlayerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;
    private final MatchService matchService;

    @GetMapping("/rankings")
    public ResponseEntity<List<Player>> getRankings(
            @RequestParam(defaultValue = "totalMatches") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) Integer limit) {
        
        List<Player> players = playerService.getRankings(sortBy, sortOrder);
        
        if (limit != null && limit > 0) {
            players = players.stream().limit(limit).toList();
        }
        
        return ResponseEntity.ok(players);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Player> getPlayer(@PathVariable String id) {
        Player player = playerService.getPlayerById(id);
        return ResponseEntity.ok(player);
    }

    @GetMapping("/{id}/matches")
    public ResponseEntity<?> getPlayerMatches(@PathVariable String id) {
        return ResponseEntity.ok(matchService.getPlayerMatches(id));
    }

    @PostMapping
    public ResponseEntity<Player> createPlayer(@Valid @RequestBody CreatePlayerRequest request) {
        Player player = playerService.createPlayer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(player);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlayer(@PathVariable String id) {
        playerService.deletePlayer(id);
        return ResponseEntity.noContent().build();
    }
}
