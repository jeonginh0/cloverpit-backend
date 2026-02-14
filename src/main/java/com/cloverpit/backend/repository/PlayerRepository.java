package com.cloverpit.backend.repository;

import com.cloverpit.backend.model.Player;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends MongoRepository<Player, String> {
    
    Optional<Player> findByPubgName(String pubgName);
    
    Optional<Player> findByPubgId(String pubgId);
    
    List<Player> findAll(Sort sort);
    
    boolean existsByPubgName(String pubgName);
}
