package com.cloverpit.backend.repository;

import com.cloverpit.backend.model.Match;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchRepository extends MongoRepository<Match, String> {
    
    List<Match> findByPlayerId(String playerId);
    
    List<Match> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    List<Match> findByPlayerIdOrderByCreatedAtDesc(String playerId);
    
    void deleteByPlayerId(String playerId);
}
