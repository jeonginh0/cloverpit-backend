package com.cloverpit.backend.repository;

import com.cloverpit.backend.model.Admin;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends MongoRepository<Admin, String> {
    
    Optional<Admin> findByUsername(String username);
    
    boolean existsByUsername(String username);
}
