package com.library.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AuthDtos {

    public record LoginRequest(
            @NotBlank(message = "Username is required")
            String username,
            
            @NotBlank(message = "Password is required")
            String password
    ) {}

    public record RegisterRequest(
            @NotBlank(message = "Username is required")
            String username,
            
            @NotBlank(message = "Password is required")
            @Size(min = 8, max = 12, message = "Password must be between 8 and 12 characters")
            String password,
            
            @NotNull(message = "Role is required")
            String role
    ) {}

    public record AuthResponse(
            String token,
            String username,
            String role,
            Long userId
    ) {}

    public record ErrorResponse(
            String error,
            String message
    ) {}
}