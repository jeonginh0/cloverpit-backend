package com.cloverpit.backend.controller;

import com.cloverpit.backend.dto.ApplicationRequest;
import com.cloverpit.backend.model.Application;
import com.cloverpit.backend.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @GetMapping
    public ResponseEntity<List<Application>> getAllApplications() {
        return ResponseEntity.ok(applicationService.getAllApplications());
    }

    @PostMapping
    public ResponseEntity<Application> createApplication(@Valid @RequestBody ApplicationRequest request) {
        Application application = applicationService.createApplication(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(application);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Application> updateApplicationStatus(
            @PathVariable String id,
            @RequestParam Application.ApplicationStatus status) {
        Application application = applicationService.updateStatus(id, status);
        return ResponseEntity.ok(application);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(@PathVariable String id) {
        applicationService.deleteApplication(id);
        return ResponseEntity.noContent().build();
    }
}
