package org.santalina.diving.service;

import org.santalina.diving.config.DivingConfig;
import org.santalina.diving.domain.User;
import org.santalina.diving.domain.UserRole;
import org.santalina.diving.dto.AuthDto.*;
import org.santalina.diving.mail.PasswordResetMailer;
import org.santalina.diving.security.JwtUtil;
import org.santalina.diving.security.PasswordUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;

import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class AuthService {

    @Inject
    JwtUtil jwtUtil;

    @Inject
    DivingConfig config;

    @Inject
    ConfigService configService;

    @Inject
    PasswordResetMailer mailer;

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (!configService.isSelfRegistration()) {
            throw new BadRequestException("Les inscriptions sont désactivées");
        }
        if (User.findByEmail(request.email()) != null) {
            throw new BadRequestException("Un compte existe déjà avec cet email");
        }
        User user = new User();
        user.email        = request.email();
        user.name         = request.name();
        user.phone        = request.phone();
        user.passwordHash = PasswordUtil.hash(request.password());
        user.role         = UserRole.DIVER;
        user.roles        = new java.util.HashSet<>();
        user.roles.add(UserRole.DIVER);
        user.persist();
        String token = jwtUtil.generateToken(user);
        return new LoginResponse(token, user.email, user.name, user.primaryRole(), user.id);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = User.findByEmail(request.email());
        if (user == null || !PasswordUtil.verify(request.password(), user.passwordHash)) {
            throw new NotAuthorizedException("Email ou mot de passe incorrect");
        }
        // Synchroniser roles depuis role si vide (migration)
        if (user.roles == null || user.roles.isEmpty()) {
            user.roles = new java.util.HashSet<>();
            user.roles.add(user.role);
            user.persist();
        }
        String token = jwtUtil.generateToken(user);
        return new LoginResponse(token, user.email, user.name, user.primaryRole(), user.id);
    }

    @Transactional
    public void requestPasswordReset(PasswordResetRequest request) {
        User user = User.findByEmail(request.email());
        if (user == null) {
            // Ne pas révéler si l'email existe ou non
            return;
        }
        String token = UUID.randomUUID().toString();
        int expiryMinutes = (config.resetTokenExpiryMinutes() > 0) ? config.resetTokenExpiryMinutes() : 30;
        user.resetToken = token;
        user.resetTokenExpiry = LocalDateTime.now().plusMinutes(expiryMinutes);
        user.persist();
        mailer.sendResetEmail(user.email, user.name, token);
    }

    @Transactional
    public void confirmPasswordReset(PasswordResetConfirm request) {
        User user = User.findByResetToken(request.token());
        if (user == null) {
            throw new NotFoundException("Token invalide");
        }
        if (user.resetTokenExpiry == null || user.resetTokenExpiry.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Token expiré");
        }
        user.passwordHash = PasswordUtil.hash(request.newPassword());
        user.resetToken = null;
        user.resetTokenExpiry = null;
        user.persist();
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = User.findByEmail(email);
        if (user == null) throw new NotFoundException("Utilisateur non trouvé");
        if (!PasswordUtil.verify(request.currentPassword(), user.passwordHash)) {
            throw new BadRequestException("Mot de passe actuel incorrect");
        }
        user.passwordHash = PasswordUtil.hash(request.newPassword());
        user.persist();
    }

    @Transactional
    public void ensureAdminExists() {
        // Ne créer l'admin par défaut que s'il n'existe aucun administrateur.
        // Ainsi, si l'admin a été supprimé intentionnellement (un autre admin existait),
        // ou si son mot de passe a été changé, il n'est ni recréé ni réinitialisé.
        if (User.countAdmins() > 0) return;

        String adminEmail = (config.adminEmail() != null && !config.adminEmail().isBlank())
                ? config.adminEmail() : "admin@santalina.com";
        String adminPassword = (config.adminPassword() != null && !config.adminPassword().isBlank())
                ? config.adminPassword() : "Admin1234";

        User admin = new User();
        admin.email        = adminEmail;
        admin.name         = "Administrateur";
        admin.passwordHash = PasswordUtil.hash(adminPassword);
        admin.role         = UserRole.ADMIN;
        admin.roles        = new java.util.HashSet<>();
        admin.roles.add(UserRole.ADMIN);
        admin.persist();
    }
}
