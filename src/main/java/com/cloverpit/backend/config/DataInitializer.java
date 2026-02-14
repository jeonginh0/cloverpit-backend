package com.cloverpit.backend.config;

import com.cloverpit.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AuthService authService;

    @Value("${spring.security.user.name}")
    private String adminUsername;

    @Value("${spring.security.user.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        try {
            authService.createAdminIfNotExists(adminUsername, adminPassword);
            log.info("✅ Admin user initialized: {}", adminUsername);
        } catch (Exception e) {
            log.error("❌ Failed to initialize admin user", e);
        }
    }
}
