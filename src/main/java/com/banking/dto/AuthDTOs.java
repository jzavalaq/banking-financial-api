package com.banking.dto;

import jakarta.validation.constraints.*;

/**
 * DTOs for Authentication operations.
 */
public class AuthDTOs {

    public record LoginRequest(
            @NotBlank(message = "Username is required")
            String username,

            @NotBlank(message = "Password is required")
            String password
    ) {}

    public record RegisterRequest(
            @NotBlank(message = "Username is required")
            @Size(min = 3, max = 50)
            String username,

            @NotBlank(message = "Password is required")
            @Size(min = 8, message = "Password must be at least 8 characters")
            @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                    message = "Password must contain uppercase, lowercase, and digit")
            String password,

            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email
    ) {}

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            Long expiresIn,
            String username,
            String role
    ) {}

    public record RefreshTokenRequest(
            @NotBlank(message = "Refresh token is required")
            String refreshToken
    ) {}

    public record LogoutRequest(
            String refreshToken
    ) {}
}
