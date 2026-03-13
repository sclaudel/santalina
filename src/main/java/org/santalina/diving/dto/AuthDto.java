package org.santalina.diving.dto;

import org.santalina.diving.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AuthDto {

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, max = 100) String password,
            @NotBlank @Size(min = 2, max = 100) String name,
            @NotBlank @Pattern(regexp = "^[+]?[0-9 .\\-()]{7,20}$", message = "Numéro de téléphone invalide")
            String phone
    ) {}

    public record LoginResponse(
            String token,
            String email,
            String name,
            UserRole role,
            Long userId,
            java.util.Set<UserRole> roles
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
