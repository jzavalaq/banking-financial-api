package com.banking.api.service;

import com.banking.api.audit.AuditLogger;
import com.banking.api.dto.AuthDTOs.*;
import com.banking.api.entity.Role;
import com.banking.api.entity.User;
import com.banking.api.exception.BadRequestException;
import com.banking.api.exception.ResourceNotFoundException;
import com.banking.api.repository.UserRepository;
import com.banking.api.security.JwtService;
import com.banking.api.security.TokenBlacklistService;
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
    private final TokenBlacklistService tokenBlacklistService;

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
    public void logout(String username, String token) {
        log.info("User logging out: {}", username);

        String clientIp = getClientIp();
        auditLogger.logSecurityEvent("LOGOUT", username, clientIp,
                "User logged out");

        // Blacklist the access token so it can't be used again
        if (token != null && !token.isEmpty()) {
            tokenBlacklistService.blacklistToken(token);
            log.info("Access token blacklisted for user: {}", username);
        }
    }

    /**
     * Legacy logout method for backward compatibility.
     * @deprecated Use logout(String username, String token) instead
     */
    @Deprecated
    @Transactional
    public void logout(String username) {
        log.warn("Using deprecated logout method - token will not be blacklisted");
        String clientIp = getClientIp();
        auditLogger.logSecurityEvent("LOGOUT", username, clientIp,
                "User logged out (legacy method)");
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

    /**
     * Blacklists a refresh token.
     * Called during logout to ensure the refresh token can't be used after logout.
     *
     * @param refreshToken The refresh token to blacklist
     */
    public void blacklistRefreshToken(String refreshToken) {
        if (refreshToken != null && !refreshToken.isEmpty()) {
            tokenBlacklistService.blacklistToken(refreshToken);
            log.info("Refresh token blacklisted");
        }
    }
}
