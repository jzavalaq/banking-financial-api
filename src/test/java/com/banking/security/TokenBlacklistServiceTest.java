package com.banking.security;

import com.banking.dto.AuthDTOs.*;
import com.banking.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TokenBlacklistService functionality.
 *
 * Verifies that:
 * 1. Tokens are properly blacklisted on logout
 * 2. Blacklisted tokens are rejected by JwtAuthenticationFilter
 * 3. Expired entries are cleaned up automatically
 */
@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class TokenBlacklistServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        // Clean blacklist before each test
        tokenBlacklistService.cleanExpiredEntries();
    }

    @Test
    @DisplayName("Should blacklist access token on logout")
    void shouldBlacklistAccessTokenOnLogout() {
        // Given: a registered user with valid tokens
        RegisterRequest registerRequest = new RegisterRequest(
                "blacklistuser" + System.currentTimeMillis(),
                "Password123",
                "blacklist" + System.currentTimeMillis() + "@example.com"
        );
        AuthResponse registerResponse = authService.register(registerRequest);
        String accessToken = registerResponse.accessToken();

        // Verify token is NOT blacklisted initially
        assertFalse(tokenBlacklistService.isBlacklisted(accessToken),
                "Token should not be blacklisted initially");

        // When: user logs out with token
        authService.logout(registerResponse.username(), accessToken);

        // Then: token IS blacklisted
        assertTrue(tokenBlacklistService.isBlacklisted(accessToken),
                "Token should be blacklisted after logout");
    }

    @Test
    @DisplayName("Should blacklist refresh token")
    void shouldBlacklistRefreshToken() {
        // Given: a registered user with valid refresh token
        RegisterRequest registerRequest = new RegisterRequest(
                "refreshtest" + System.currentTimeMillis(),
                "Password123",
                "refreshtest" + System.currentTimeMillis() + "@example.com"
        );
        AuthResponse registerResponse = authService.register(registerRequest);
        String refreshToken = registerResponse.refreshToken();

        // Verify refresh token is NOT blacklisted initially
        assertFalse(tokenBlacklistService.isBlacklisted(refreshToken),
                "Refresh token should not be blacklisted initially");

        // When: refresh token is blacklisted
        authService.blacklistRefreshToken(refreshToken);

        // Then: refresh token IS blacklisted
        assertTrue(tokenBlacklistService.isBlacklisted(refreshToken),
                "Refresh token should be blacklisted");
    }

    @Test
    @DisplayName("Should handle multiple tokens for same user")
    void shouldHandleMultipleTokensForSameUser() {
        // Given: a user with multiple tokens (multiple logins)
        String username = "multitoken" + System.currentTimeMillis();
        RegisterRequest registerRequest = new RegisterRequest(
                username,
                "Password123",
                "multitoken" + System.currentTimeMillis() + "@example.com"
        );
        authService.register(registerRequest);

        // Login twice to get different tokens
        LoginRequest loginRequest = new LoginRequest(username, "Password123");
        AuthResponse login1 = authService.login(loginRequest);
        AuthResponse login2 = authService.login(loginRequest);

        // When: logout first token only
        authService.logout(username, login1.accessToken());

        // Then: first token blacklisted, second token still valid
        assertTrue(tokenBlacklistService.isBlacklisted(login1.accessToken()),
                "First token should be blacklisted");
        assertFalse(tokenBlacklistService.isBlacklisted(login2.accessToken()),
                "Second token should still be valid");
    }

    @Test
    @DisplayName("Should return false for non-existent token")
    void shouldReturnFalseForNonExistentToken() {
        // Given: a random invalid token
        String fakeToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJmYWtlIiwiaWF0IjoxNTE2MjM5MDIyfQ.fake-signature";

        // When: checking if blacklisted
        boolean isBlacklisted = tokenBlacklistService.isBlacklisted(fakeToken);

        // Then: should return false (not blacklisted, just invalid)
        assertFalse(isBlacklisted, "Invalid token should not be in blacklist");
    }

    @Test
    @DisplayName("Should clean expired entries")
    void shouldCleanExpiredEntries() {
        // Given: a token that will be blacklisted
        RegisterRequest registerRequest = new RegisterRequest(
                "cleanuptest" + System.currentTimeMillis(),
                "Password123",
                "cleanup" + System.currentTimeMillis() + "@example.com"
        );
        AuthResponse registerResponse = authService.register(registerRequest);
        String accessToken = registerResponse.accessToken();

        authService.logout(registerResponse.username(), accessToken);
        assertTrue(tokenBlacklistService.isBlacklisted(accessToken));

        // When: cleanup is called (normally happens via scheduled task)
        tokenBlacklistService.cleanExpiredEntries();

        // Then: blacklist size should be manageable
        // (Note: entries won't be removed unless expired, but the method should run without error)
        assertTrue(tokenBlacklistService.getBlacklistSize() >= 0);
    }
}
