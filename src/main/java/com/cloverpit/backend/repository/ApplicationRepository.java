package com.cloverpit.backend.repository;

import com.cloverpit.backend.model.Application;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationRepository extends MongoRepository<Application, String> {
    
    List<Application> findAllByOrderByCreatedAtDesc();
    
    List<Application> findByStatus(Application.ApplicationStatus status);
}
