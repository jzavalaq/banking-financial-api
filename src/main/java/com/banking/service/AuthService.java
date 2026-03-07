package com.banking.service;

import com.banking.audit.AuditLogger;
import com.banking.dto.AuthDTOs.*;
import com.banking.entity.Role;
import com.banking.entity.User;
import com.banking.exception.BadRequestException;
import com.banking.exception.ResourceNotFoundException;
import com.banking.repository.UserRepository;
import com.banking.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
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
 *
 * Handles user registration, login, token generation, and session management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuditLogger auditLogger;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private HttpServletRequest request;

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
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
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

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );

            User user = (User) authentication.getPrincipal();

            String accessToken = jwtService.generateToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            log.info("User logged in successfully: {}", user.getUsername());

            // Log successful login
            String clientIp = getClientIp();
            auditLogger.logSecurityEvent("LOGIN_SUCCESS", user.getUsername(), clientIp,
                    "User authenticated successfully");

            return new AuthResponse(
                    accessToken,
                    refreshToken,
                    "Bearer",
                    900000L,
                    user.getUsername(),
                    user.getRole().name()
            );
        } catch (Exception e) {
            // Log failed login attempt
            String clientIp = getClientIp();
            auditLogger.logSecurityEvent("LOGIN_FAILURE", request.username(), clientIp,
                    "Authentication failed: " + e.getMessage());
            throw e;
        }
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.info("Refreshing token");

        String username = jwtService.extractUsername(request.refreshToken());

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        if (!jwtService.isTokenValid(request.refreshToken(), user)) {
            auditLogger.logSecurityEvent("TOKEN_REFRESH_FAILURE", username, getClientIp(),
                    "Invalid refresh token");
            throw new BadRequestException("Invalid refresh token");
        }

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        auditLogger.logSecurityEvent("TOKEN_REFRESH_SUCCESS", username, getClientIp(),
                "Token refreshed successfully");

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

        String clientIp = getClientIp();
        auditLogger.logSecurityEvent("LOGOUT", username, clientIp,
                "User logged out");

        // In a real implementation, you would invalidate the refresh token
        // by adding it to a blacklist or removing from database
    }

    private String getClientIp() {
        if (request == null) {
            return "unknown";
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
