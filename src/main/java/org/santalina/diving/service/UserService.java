package org.santalina.diving.service;

import org.santalina.diving.domain.User;
import org.santalina.diving.domain.UserRole;
import org.santalina.diving.dto.AuthDto.LoginResponse;
import org.santalina.diving.dto.UserDto.*;
import org.santalina.diving.security.JwtUtil;
import org.santalina.diving.security.PasswordUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import org.jboss.logging.Logger;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

@ApplicationScoped
public class UserService {

    private static final Logger LOG = Logger.getLogger(UserService.class);

    @Inject
    JwtUtil jwtUtil;

    public UserResponse getProfile(String email) {
        User user = User.findByEmail(email);
        if (user == null) throw new NotFoundException("Utilisateur non trouvé");
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = User.findByEmail(email);
        if (user == null) throw new NotFoundException("Utilisateur non trouvé");
        user.firstName     = request.firstName().trim();
        user.lastName      = request.lastName().trim().toUpperCase();
        user.phone         = normalizePhone(request.phone());
        user.licenseNumber = (request.licenseNumber() != null && !request.licenseNumber().isBlank()) ? request.licenseNumber().trim() : null;
        user.club          = request.club() != null && !request.club().isBlank() ? request.club().trim() : null;
        user.persist();
        return UserResponse.from(user);
    }

    /** Met à jour l'email de l'utilisateur et retourne un nouveau token JWT. */
    @Transactional
    public LoginResponse updateEmail(String currentEmail, UpdateEmailRequest request) {
        String newEmail = request.email().trim().toLowerCase();
        if (newEmail.equalsIgnoreCase(currentEmail)) {
            throw new BadRequestException("L'email est identique au précédent");
        }
        if (User.findByEmail(newEmail) != null) {
            throw new BadRequestException("Cet email est déjà utilisé par un autre compte");
        }
        User user = User.findByEmail(currentEmail);
        if (user == null) throw new NotFoundException("Utilisateur non trouvé");
        user.email = newEmail;
        user.persist();
        LOG.infof("Email mis à jour : %s → %s", currentEmail, newEmail);
        String token = jwtUtil.generateToken(user);
        return new LoginResponse(token, user.email, user.firstName, user.lastName,
                user.primaryRole(), user.id, user.roles, user.phone, user.licenseNumber, user.club);
    }

    public List<UserResponse> getAllUsers() {
        return User.<User>listAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    /** Recherche d'utilisateurs par nom ou email (insensible à la casse ET aux accents, max 10 résultats) */
    public List<UserSearchResult> searchUsers(String query) {
        if (query == null || query.isBlank()) return List.of();
        String normalized = stripAccents(query.trim().toLowerCase());
        return User.<User>listAll()
                .stream()
                .filter(u -> stripAccents(u.fullName().toLowerCase()).contains(normalized)
                          || stripAccents(u.email.toLowerCase()).contains(normalized))
                .limit(10)
                .map(UserSearchResult::from)
                .toList();
    }

    public List<UserSearchResult> getDiveDirectors() {
        return User.<User>listAll()
                .stream()
                .filter(u -> u.activated && u.roles.contains(UserRole.DIVE_DIRECTOR))
                .sorted(Comparator.comparing((User u) -> u.lastName)
                                  .thenComparing(u -> u.firstName))
                .map(UserSearchResult::from)
                .toList();
    }

    /** Supprime les diacritiques (accents) d'une chaîne */
    private static String stripAccents(String s) {
        if (s == null) return "";
        String decomposed = Normalizer.normalize(s, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "");
    }

    @Transactional
    public UserResponse updateRoles(Long userId, UpdateRolesRequest request) {
        User user = User.findById(userId);
        if (user == null) throw new NotFoundException("Utilisateur non trouvé");
        if (user.hasRole(UserRole.ADMIN)
                && !request.roles().contains(UserRole.ADMIN)
                && User.countAdmins() <= 1) {
            LOG.warnf("Tentative de retrait du rôle admin du dernier administrateur (userId=%d)", userId);
            throw new BadRequestException("Impossible de retirer le rôle administrateur du dernier administrateur");
        }
        user.roles = new HashSet<>(request.roles());
        user.role  = user.primaryRole(); // synchroniser le rôle principal
        user.persist();
        LOG.infof("Rôles mis à jour pour userId=%d : %s", userId, user.roles);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (User.findByEmail(request.email()) != null) {
            LOG.warnf("Tentative de création avec un email déjà utilisé : %s", request.email());
            throw new BadRequestException("Un compte existe déjà avec cet email");
        }
        User user = new User();
        user.email        = request.email();
        user.firstName    = request.firstName().trim();
        user.lastName     = request.lastName().trim().toUpperCase();
        user.phone        = normalizePhone(request.phone());
        user.licenseNumber = (request.licenseNumber() != null && !request.licenseNumber().isBlank()) ? request.licenseNumber().trim() : null;
        user.club          = request.club() != null && !request.club().isBlank() ? request.club().trim() : null;
        user.passwordHash = PasswordUtil.hash(request.password());
        user.roles        = new HashSet<>(request.roles());
        user.role         = user.primaryRole();
        user.activated    = true;
        user.persist();
        LOG.infof("Utilisateur créé par admin : %s (roles=%s)", user.email, user.roles);
        return UserResponse.from(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = User.findById(userId);
        if (user == null) throw new NotFoundException("Utilisateur non trouvé");
        if (user.hasRole(UserRole.ADMIN) && User.countAdmins() <= 1) {
            LOG.warnf("Tentative de suppression du dernier administrateur (userId=%d)", userId);
            throw new BadRequestException("Impossible de supprimer le dernier administrateur");
        }
        LOG.infof("Suppression de l'utilisateur : %s (userId=%d)", user.email, userId);
        user.delete();
    }

    @Transactional
    public UserResponse updateUserAsAdmin(Long userId, UpdateUserAdminRequest request) {
        User user = User.findById(userId);
        if (user == null) throw new NotFoundException("Utilisateur non trouvé");
        if (!user.email.equals(request.email())) {
            if (User.findByEmail(request.email()) != null) {
                throw new BadRequestException("Un compte existe déjà avec cet email");
            }
        }
        user.email     = request.email();
        user.firstName = request.firstName().trim();
        user.lastName  = request.lastName().trim().toUpperCase();
        user.phone     = normalizePhone(request.phone());
        user.licenseNumber = (request.licenseNumber() != null && !request.licenseNumber().isBlank()) ? request.licenseNumber().trim() : null;
        user.club          = request.club() != null && !request.club().isBlank() ? request.club().trim() : null;
        user.persist();
        return UserResponse.from(user);
    }

    /** Normalise un numéro de téléphone français au format +33XXXXXXXXX */
    private static String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) return null;
        String clean = phone.trim().replaceAll("[\\s.\\-()]", "");
        if (clean.startsWith("0")) return "+33" + clean.substring(1);
        return clean;
    }
    @Transactional
    public UserResponse updateNotifPrefs(String email, UpdateNotifPrefsRequest request) {
        User user = User.findByEmail(email);
        if (user == null) throw new NotFoundException("Utilisateur non trouvé");
        user.notifOnRegistration    = request.notifOnRegistration();
        user.notifOnApproved        = request.notifOnApproved();
        user.notifOnCancelled       = request.notifOnCancelled();
        user.notifOnMovedToWaitlist = request.notifOnMovedToWaitlist();        user.notifOnDpRegistration  = request.notifOnDpRegistration();        user.notifOnCreatorRegistration = request.notifOnCreatorRegistration();        user.notifOnSafetyReminder = request.notifOnSafetyReminder();        user.persist();
        return UserResponse.from(user);
    }

    /** Met à jour le modèle d'email d'organisation du directeur de plongée. */
    @Transactional
    public UserResponse updateDpEmailTemplate(String email, UpdateDpEmailTemplateRequest request) {
        User user = User.findByEmail(email);
        if (user == null) throw new NotFoundException("Utilisateur non trouvé");
        user.dpOrganizerEmailTemplate = request.template();
        user.persist();
        return UserResponse.from(user);
    }

    /** Exporte tous les utilisateurs au format CSV (séparateur point-virgule), triés par club. */
    public String exportUsersCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("club;nom;prenom;email;telephone;licence\n");
        User.<User>listAll().stream()
            .sorted(Comparator.comparing(
                (User u) -> u.club != null ? u.club : "\uFFFF",
                String::compareTo))
            .forEach(u -> sb
                .append(csvEscape(u.club)).append(";")
                .append(csvEscape(u.lastName)).append(";")
                .append(csvEscape(u.firstName)).append(";")
                .append(csvEscape(u.email)).append(";")
                .append(csvEscape(u.phone)).append(";")
                .append(csvEscape(u.licenseNumber)).append("\n"));
        return sb.toString();
    }

    /** Importe des utilisateurs depuis un CSV (séparateur ;). Ignore les emails déjà existants. */
    @Transactional
    public CsvImportResult importUsersCsv(CsvImportRequest request) {
        int imported = 0, skipped = 0, errors = 0;
        List<String> messages = new ArrayList<>();
        String[] lines = request.csvContent().split("\\r?\\n");
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            String[] cols = parseCsvLine(line);
            if (cols.length < 4) {
                errors++;
                messages.add("Ligne " + (i + 1) + " ignorée (format invalide, moins de 4 colonnes)");
                continue;
            }
            String club       = cols[0].trim();
            String lastName   = cols[1].trim();
            String firstName  = cols[2].trim();
            String email      = cols[3].trim().toLowerCase();
            String phone      = cols.length > 4 ? cols[4].trim() : "";
            String licenseNum = cols.length > 5 ? cols[5].trim() : "";
            if (email.isEmpty()) {
                errors++;
                messages.add("Ligne " + (i + 1) + " ignorée (email manquant)");
                continue;
            }
            if (User.findByEmail(email) != null) {
                skipped++;
                messages.add("Ignoré (email déjà existant) : " + email);
                continue;
            }
            User user = new User();
            user.email         = email;
            user.firstName     = firstName.isEmpty() ? "" : firstName;
            user.lastName      = lastName.isEmpty() ? "" : lastName;
            user.phone         = normalizePhone(phone.isEmpty() ? null : phone);
            user.licenseNumber = licenseNum.isEmpty() ? null : licenseNum;
            user.club          = club.isEmpty() ? null : club;
            user.passwordHash  = PasswordUtil.hash(request.password());
            user.roles         = new HashSet<>(java.util.Set.of(UserRole.DIVER));
            user.activated     = true;
            user.persist();
            imported++;
            LOG.infof("Utilisateur importé via CSV : %s", email);
        }
        LOG.infof("Import CSV terminé : %d importés, %d ignorés, %d erreurs", imported, skipped, errors);
        return new CsvImportResult(imported, skipped, errors, messages);
    }

    private static String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ';') {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }

    private static String csvEscape(String value) {
        if (value == null || value.isEmpty()) return "";
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
