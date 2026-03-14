package com.banking.service;

import com.banking.dto.AuthDTOs.*;
import com.banking.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Test
    @DisplayName("Should authenticate with valid credentials")
    void shouldAuthenticateWithValidCredentials() {
        RegisterRequest registerRequest = new RegisterRequest(
                "testuser" + System.currentTimeMillis(),
                "Password123",
                "test" + System.currentTimeMillis() + "@example.com"
        );

        AuthResponse registerResponse = authService.register(registerRequest);

        LoginRequest loginRequest = new LoginRequest(
                registerRequest.username(),
                registerRequest.password()
        );

        AuthResponse loginResponse = authService.login(loginRequest);

        assertNotNull(loginResponse.accessToken());
        assertNotNull(loginResponse.refreshToken());
        assertEquals("Bearer", loginResponse.tokenType());
        assertEquals(registerRequest.username(), loginResponse.username());
    }

    @Test
    @DisplayName("Should reject invalid credentials")
    void shouldRejectInvalidCredentials() {
        LoginRequest loginRequest = new LoginRequest(
                "nonexistentuser",
                "wrongpassword"
        );

        assertThrows(Exception.class, () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("Should refresh valid token")
    void shouldRefreshValidToken() {
        RegisterRequest registerRequest = new RegisterRequest(
                "refreshuser" + System.currentTimeMillis(),
                "Password123",
                "refresh" + System.currentTimeMillis() + "@example.com"
        );

        AuthResponse registerResponse = authService.register(registerRequest);

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(
                registerResponse.refreshToken()
        );

        AuthResponse refreshResponse = authService.refreshToken(refreshRequest);

        assertNotNull(refreshResponse.accessToken());
        assertNotNull(refreshResponse.refreshToken());
    }

    @Test
    @DisplayName("Should logout and invalidate refresh token")
    void shouldLogoutAndInvalidateRefreshToken() {
        RegisterRequest registerRequest = new RegisterRequest(
                "logoutuser" + System.currentTimeMillis(),
                "Password123",
                "logout" + System.currentTimeMillis() + "@example.com"
        );

        AuthResponse registerResponse = authService.register(registerRequest);

        assertDoesNotThrow(() -> authService.logout(registerResponse.username()));
    }

    @Test
    @DisplayName("Should reject duplicate username on registration")
    void shouldRejectExpiredRefreshToken() {
        String username = "duplicate" + System.currentTimeMillis();

        RegisterRequest request1 = new RegisterRequest(
                username,
                "Password123",
                "email1" + System.currentTimeMillis() + "@example.com"
        );

        authService.register(request1);

        RegisterRequest request2 = new RegisterRequest(
                username,
                "Password456",
                "email2" + System.currentTimeMillis() + "@example.com"
        );

        assertThrows(BadRequestException.class, () -> authService.register(request2));
    }
}
