package com.cloverpit.backend.service;

import com.cloverpit.backend.dto.LoginRequest;
import com.cloverpit.backend.dto.LoginResponse;
import com.cloverpit.backend.model.Admin;
import com.cloverpit.backend.repository.AdminRepository;
import com.cloverpit.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        String token = jwtTokenProvider.generateToken(request.getUsername());

        return LoginResponse.builder()
                .token(token)
                .username(request.getUsername())
                .build();
    }

    public void createAdminIfNotExists(String username, String password) {
        if (!adminRepository.existsByUsername(username)) {
            Admin admin = Admin.builder()
                    .username(username)
                    .password(passwordEncoder.encode(password))
                    .role("ADMIN")
                    .build();
            adminRepository.save(admin);
        }
    }
}
