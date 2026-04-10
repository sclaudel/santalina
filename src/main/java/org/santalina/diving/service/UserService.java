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
        user.licenseNumber = request.licenseNumber() != null ? request.licenseNumber().trim() : null;
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
                user.primaryRole(), user.id, user.roles);
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
        user.licenseNumber = request.licenseNumber() != null ? request.licenseNumber().trim() : null;
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
        user.licenseNumber = request.licenseNumber() != null ? request.licenseNumber().trim() : null;
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
        if (user == null) throw new NotFoundException("Utilisateur non trouv\u00e9");
        user.notifOnRegistration    = request.notifOnRegistration();
        user.notifOnApproved        = request.notifOnApproved();
        user.notifOnCancelled       = request.notifOnCancelled();
        user.notifOnMovedToWaitlist = request.notifOnMovedToWaitlist();        user.notifOnDpRegistration  = request.notifOnDpRegistration();        user.persist();
        return UserResponse.from(user);
    }}
