package org.santalina.diving.service;

import org.santalina.diving.domain.User;
import org.santalina.diving.domain.UserRole;
import org.santalina.diving.dto.UserDto.*;
import org.santalina.diving.security.PasswordUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;

@ApplicationScoped
public class UserService {

    public UserResponse getProfile(String email) {
        User user = User.findByEmail(email);
        if (user == null) throw new NotFoundException("Utilisateur non trouvé");
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = User.findByEmail(email);
        if (user == null) throw new NotFoundException("Utilisateur non trouvé");
        user.name  = request.name();
        user.phone = request.phone();
        user.persist();
        return UserResponse.from(user);
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
                .filter(u -> stripAccents(u.name.toLowerCase()).contains(normalized)
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
            throw new BadRequestException("Impossible de retirer le rôle administrateur du dernier administrateur");
        }
        user.roles = new HashSet<>(request.roles());
        user.role  = user.primaryRole(); // synchroniser le rôle principal
        user.persist();
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (User.findByEmail(request.email()) != null) {
            throw new BadRequestException("Un compte existe déjà avec cet email");
        }
        User user = new User();
        user.email        = request.email();
        user.name         = request.name();
        user.phone        = request.phone();
        user.passwordHash = PasswordUtil.hash(request.password());
        user.roles        = new HashSet<>(request.roles());
        user.role         = user.primaryRole();
        user.persist();
        return UserResponse.from(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = User.findById(userId);
        if (user == null) throw new NotFoundException("Utilisateur non trouvé");
        if (user.hasRole(UserRole.ADMIN) && User.countAdmins() <= 1) {
            throw new BadRequestException("Impossible de supprimer le dernier administrateur");
        }
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
        user.email = request.email();
        user.name  = request.name();
        user.phone = request.phone();
        user.persist();
        return UserResponse.from(user);
    }
}
