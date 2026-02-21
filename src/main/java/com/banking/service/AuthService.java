package com.banking.service;

import com.banking.dto.AuthDTOs.*;
import com.banking.entity.Role;
import com.banking.entity.User;
import com.banking.exception.BadRequestException;
import com.banking.exception.ResourceNotFoundException;
import com.banking.repository.UserRepository;
import com.banking.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for Authentication operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.username());

        if (userRepository.existsByUsername(request.username())) {
            throw new BadRequestException("Username already exists");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email already exists");
        }

        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .email(request.email())
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();

        user = userRepository.save(user);

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        log.info("User registered successfully: {}", user.getUsername());

        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                900000L,
                user.getUsername(),
                user.getRole().name()
        );
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.username());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        User user = (User) authentication.getPrincipal();

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        log.info("User logged in successfully: {}", user.getUsername());

        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                900000L,
                user.getUsername(),
                user.getRole().name()
        );
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.info("Refreshing token");

        String username = jwtService.extractUsername(request.refreshToken());

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        if (!jwtService.isTokenValid(request.refreshToken(), user)) {
            throw new BadRequestException("Invalid refresh token");
        }

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                900000L,
                user.getUsername(),
                user.getRole().name()
        );
    }

    @Transactional
    public void logout(String username) {
        log.info("User logging out: {}", username);
        // In a real implementation, you would invalidate the refresh token
        // by adding it to a blacklist or removing from database
    }
}
