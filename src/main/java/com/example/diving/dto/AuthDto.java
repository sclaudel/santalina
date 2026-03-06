package com.example.diving.dto;

import com.example.diving.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDto {

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, max = 100) String password,
            @NotBlank @Size(min = 2, max = 100) String name
    ) {}

    public record LoginResponse(
            String token,
            String email,
            String name,
            UserRole role,
            Long userId
    ) {}

    public record PasswordResetRequest(
            @NotBlank @Email String email
    ) {}

    public record PasswordResetConfirm(
            @NotBlank String token,
            @NotBlank @Size(min = 6, max = 100) String newPassword
    ) {}

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 6, max = 100) String newPassword
    ) {}
}

