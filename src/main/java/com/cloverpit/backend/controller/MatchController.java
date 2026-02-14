package com.cloverpit.backend.controller;

import com.cloverpit.backend.model.Match;
import com.cloverpit.backend.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @GetMapping("/recent")
    public ResponseEntity<List<Match>> getRecentMatches(
            @RequestParam(defaultValue = "10") int limit) {
        List<Match> matches = matchService.getRecentMatches(limit);
        return ResponseEntity.ok(matches);
    }
}
