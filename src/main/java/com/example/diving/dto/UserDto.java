package com.example.diving.dto;

import com.example.diving.domain.User;
import com.example.diving.domain.UserRole;
import jakarta.validation.constraints.*;

import java.util.Set;

public class UserDto {

    public record UserResponse(
            Long id,
            String email,
            String name,
            String phone,
            UserRole role,          // rôle principal
            Set<UserRole> roles     // tous les rôles
    ) {
        public static UserResponse from(User user) {
            return new UserResponse(
                    user.id, user.email, user.name, user.phone,
                    user.primaryRole(),
                    user.roles != null ? user.roles : Set.of(user.role)
            );
        }
    }

    public record UpdateProfileRequest(
            @NotBlank @Size(min = 2, max = 100) String name,
            @Pattern(regexp = "^[+]?[0-9 .\\-()]{7,20}$", message = "Numéro de téléphone invalide")
            String phone
    ) {}

    /** Remplace UpdateRoleRequest — supporte maintenant plusieurs rôles */
    public record UpdateRolesRequest(
            @NotNull @Size(min = 1) Set<UserRole> roles
    ) {}

    public record CreateUserRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 2, max = 100) String name,
            @NotBlank @Size(min = 6, max = 100) String password,
            @Pattern(regexp = "^[+]?[0-9 .\\-()]{7,20}$", message = "Numéro de téléphone invalide")
            String phone,
            @NotNull @Size(min = 1) Set<UserRole> roles
    ) {}

    /** Résultat allégé pour la recherche lors de l'ajout d'un plongeur */
    public record UserSearchResult(
            Long id,
            String name,
            String email,
            String phone
    ) {
        public static UserSearchResult from(User user) {
            return new UserSearchResult(user.id, user.name, user.email, user.phone);
        }
    }
}
