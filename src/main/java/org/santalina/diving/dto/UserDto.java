package org.santalina.diving.dto;

import org.santalina.diving.domain.User;
import org.santalina.diving.domain.UserRole;
import jakarta.validation.constraints.*;

import java.util.Set;

public class UserDto {

    public record UserResponse(
            Long id,
            String email,
            String firstName,
            String lastName,
            String name,
            String phone,
            UserRole role,
            Set<UserRole> roles
    ) {
        public static UserResponse from(User user) {
            return new UserResponse(
                    user.id, user.email, user.firstName, user.lastName,
                    user.fullName(), user.phone,
                    user.primaryRole(),
                    user.roles != null ? user.roles : Set.of(user.role)
            );
        }
    }

    public record UpdateProfileRequest(
            @NotBlank @Size(min = 2, max = 100) String firstName,
            @NotBlank @Size(min = 2, max = 100) String lastName,
            @Pattern(regexp = "^[+]?[0-9 .\\-()]{7,20}$", message = "Numéro de téléphone invalide")
            String phone
    ) {}

    /** Remplace UpdateRoleRequest — supporte maintenant plusieurs rôles */
    public record UpdateRolesRequest(
            @NotNull @Size(min = 1) Set<UserRole> roles
    ) {}

    public record CreateUserRequest(
            @NotBlank(message = "L'email est obligatoire") @Email(message = "Format email invalide") String email,
            @NotBlank(message = "Le prénom est obligatoire") @Size(min = 2, max = 100) String firstName,
            @NotBlank(message = "Le nom est obligatoire") @Size(min = 2, max = 100) String lastName,
            @NotBlank(message = "Le mot de passe est obligatoire") @Size(min = 6, max = 100, message = "Le mot de passe doit faire au moins 6 caractères") String password,
            @Pattern(regexp = "^[+]?[0-9 .\\-()]{7,20}$", message = "Numéro de téléphone invalide")
            String phone,
            @NotNull @Size(min = 1) Set<UserRole> roles
    ) {}

    public record UpdateUserAdminRequest(
            @NotBlank(message = "L'email est obligatoire") @Email(message = "Format email invalide") String email,
            @NotBlank(message = "Le prénom est obligatoire") @Size(min = 2, max = 100) String firstName,
            @NotBlank(message = "Le nom est obligatoire") @Size(min = 2, max = 100) String lastName,
            @Pattern(regexp = "^[+]?[0-9 .\\-()]{7,20}$", message = "Numéro de téléphone invalide")
            String phone
    ) {}

    public record UserSearchResult(
            Long id,
            String firstName,
            String lastName,
            String name,
            String email,
            String phone
    ) {
        public static UserSearchResult from(User user) {
            return new UserSearchResult(
                    user.id, user.firstName, user.lastName,
                    user.fullName(), user.email, user.phone
            );
        }
    }
}
