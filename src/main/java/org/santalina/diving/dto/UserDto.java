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
            String licenseNumber,
            String club,
            UserRole role,
            Set<UserRole> roles,
            boolean notifOnRegistration,
            boolean notifOnApproved,
            boolean notifOnCancelled,
            boolean notifOnMovedToWaitlist,
            boolean notifOnDpRegistration,
            boolean notifOnCreatorRegistration,
            boolean notifOnSafetyReminder
    ) {
        public static UserResponse from(User user) {
            return new UserResponse(
                    user.id, user.email, user.firstName, user.lastName,
                    user.fullName(), user.phone, user.licenseNumber, user.club,
                    user.primaryRole(),
                    user.roles != null ? user.roles : Set.of(user.role),
                    user.notifOnRegistration,
                    user.notifOnApproved,
                    user.notifOnCancelled,
                    user.notifOnMovedToWaitlist,
                    user.notifOnDpRegistration,
                    user.notifOnCreatorRegistration,
                    user.notifOnSafetyReminder
            );
        }
    }

    public record UpdateProfileRequest(
            @NotBlank @Size(min = 2, max = 100) String firstName,
            @NotBlank @Size(min = 2, max = 100) String lastName,
            @Pattern(regexp = "^(0[1-9][0-9]{8}|\\+33[1-9][0-9]{8})$", message = "Numéro de téléphone français invalide (ex: 0612345678 ou +33612345678)")
            String phone,
            @Pattern(regexp = "^[A-Z]-\\d{2}-\\d{6,10}$", message = "Format invalide (ex: A-14-1223422222)")
            @Size(max = 20) String licenseNumber,
            String club
    ) {}

    /** Mise à jour de l'email de l'utilisateur connecté. Retourne un nouveau token JWT. */
    public record UpdateEmailRequest(
            @NotBlank @Email String email
    ) {}

    /** Réplace UpdateRoleRequest — supporte maintenant plusieurs rôles */
    public record UpdateRolesRequest(
            @NotNull @Size(min = 1) Set<UserRole> roles
    ) {}

    /** Mise à jour des préférences de notifications e-mail de l'utilisateur connecté. */
    public record UpdateNotifPrefsRequest(
            @NotNull Boolean notifOnRegistration,
            @NotNull Boolean notifOnApproved,
            @NotNull Boolean notifOnCancelled,
            @NotNull Boolean notifOnMovedToWaitlist,
            @NotNull Boolean notifOnDpRegistration,
            @NotNull Boolean notifOnCreatorRegistration,
            @NotNull Boolean notifOnSafetyReminder
    ) {}

    public record CreateUserRequest(
            @NotBlank(message = "L'email est obligatoire") @Email(message = "Format email invalide") String email,
            @NotBlank(message = "Le prénom est obligatoire") @Size(min = 2, max = 100) String firstName,
            @NotBlank(message = "Le nom est obligatoire") @Size(min = 2, max = 100) String lastName,
            @NotBlank(message = "Le mot de passe est obligatoire") @Size(min = 6, max = 100, message = "Le mot de passe doit faire au moins 6 caractères") String password,
            @Pattern(regexp = "^(0[1-9][0-9]{8}|\\+33[1-9][0-9]{8})$", message = "Numéro de téléphone français invalide (ex: 0612345678 ou +33612345678)")
            String phone,
            @Pattern(regexp = "^[A-Z]-\\d{2}-\\d{6,10}$", message = "Format invalide (ex: A-14-1223422222)")
            @Size(max = 20) String licenseNumber,
            String club,
            @NotNull @Size(min = 1) Set<UserRole> roles
    ) {}

    public record UpdateUserAdminRequest(
            @NotBlank(message = "L'email est obligatoire") @Email(message = "Format email invalide") String email,
            @NotBlank(message = "Le prénom est obligatoire") @Size(min = 2, max = 100) String firstName,
            @NotBlank(message = "Le nom est obligatoire") @Size(min = 2, max = 100) String lastName,
            @Pattern(regexp = "^(0[1-9][0-9]{8}|\\+33[1-9][0-9]{8})$", message = "Numéro de téléphone français invalide (ex: 0612345678 ou +33612345678)")
            String phone,
            @Pattern(regexp = "^[A-Z]-\\d{2}-\\d{6,10}$", message = "Format invalide (ex: A-14-1223422222)")
            @Size(max = 20) String licenseNumber,
            String club
    ) {}

    public record UserSearchResult(
            Long id,
            String firstName,
            String lastName,
            String name,
            String email,
            String phone,
            String licenseNumber
    ) {
        public static UserSearchResult from(User user) {
            return new UserSearchResult(
                    user.id, user.firstName, user.lastName,
                    user.fullName(), user.email, user.phone, user.licenseNumber
            );
        }
    }

    /** Résultat d'un import CSV d'utilisateurs. */
    public record CsvImportResult(
            int imported,
            int skipped,
            int errors,
            java.util.List<String> messages
    ) {}

    /** Requête d'import CSV d'utilisateurs (corps JSON). */
    public record CsvImportRequest(
            @jakarta.validation.constraints.NotBlank String csvContent,
            @jakarta.validation.constraints.NotBlank
            @jakarta.validation.constraints.Size(min = 6, max = 100) String password
    ) {}
}
