package com.banking.api.integration;

import com.banking.api.dto.AuthDTOs.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/auth/register - should register new user")
    void register_validRequest_returns200() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "testuser" + System.currentTimeMillis(),
                "Password123",
                "test" + System.currentTimeMillis() + "@example.com"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value(request.username()));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - should return 400 for duplicate username")
    void register_duplicateUsername_returns400() throws Exception {
        String username = "duplicate" + System.currentTimeMillis();

        RegisterRequest request1 = new RegisterRequest(
                username, "Password123", "email1" + System.currentTimeMillis() + "@example.com"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());

        // Try to register with same username
        RegisterRequest request2 = new RegisterRequest(
                username, "Password456", "email2" + System.currentTimeMillis() + "@example.com"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - should login with valid credentials")
    void login_validCredentials_returns200() throws Exception {
        // Register user first
        RegisterRequest registerRequest = new RegisterRequest(
                "loginuser" + System.currentTimeMillis(),
                "Password123",
                "login" + System.currentTimeMillis() + "@example.com"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        // Login
        LoginRequest loginRequest = new LoginRequest(registerRequest.username(), registerRequest.password());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - should return 401 for invalid credentials")
    void login_invalidCredentials_returns401() throws Exception {
        LoginRequest loginRequest = new LoginRequest("nonexistentuser", "wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - should refresh valid token")
    void refreshToken_validToken_returns200() throws Exception {
        // Register user
        RegisterRequest registerRequest = new RegisterRequest(
                "refreshuser" + System.currentTimeMillis(),
                "Password123",
                "refresh" + System.currentTimeMillis() + "@example.com"
        );

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(registerResult.getResponse().getContentAsString(), AuthResponse.class);

        // Refresh token
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(authResponse.refreshToken());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout - should logout successfully")
    void logout_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk());
    }
}
