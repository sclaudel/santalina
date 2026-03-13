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

import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class AuthService {

    private static final Logger LOG = Logger.getLogger(AuthService.class);

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
            LOG.warnf("Tentative d'inscription refusée (inscriptions désactivées) pour %s", request.email());
            throw new BadRequestException("Les inscriptions sont désactivées");
        }
        if (User.findByEmail(request.email()) != null) {
            LOG.warnf("Tentative d'inscription avec un email déjà utilisé : %s", request.email());
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
        LOG.infof("Nouvel utilisateur inscrit : %s", user.email);
        String token = jwtUtil.generateToken(user);
        return new LoginResponse(token, user.email, user.name, user.primaryRole(), user.id, user.roles);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = User.findByEmail(request.email());
        if (user == null || !PasswordUtil.verify(request.password(), user.passwordHash)) {
            LOG.warnf("Échec de connexion pour : %s", request.email());
            throw new NotAuthorizedException("Email ou mot de passe incorrect");
        }
        // Synchroniser roles depuis role si vide (migration)
        if (user.roles == null || user.roles.isEmpty()) {
            user.roles = new java.util.HashSet<>();
            user.roles.add(user.role);
            user.persist();
        }
        LOG.infof("Connexion réussie pour : %s", user.email);
        String token = jwtUtil.generateToken(user);
        return new LoginResponse(token, user.email, user.name, user.primaryRole(), user.id, user.roles);
    }

    @Transactional
    public void requestPasswordReset(PasswordResetRequest request) {
        User user = User.findByEmail(request.email());
        if (user == null) {
            // Ne pas révéler si l'email existe ou non
            LOG.debugf("Demande de réinitialisation pour email inconnu : %s", request.email());
            return;
        }
        String token = UUID.randomUUID().toString();
        int expiryMinutes = (config.resetTokenExpiryMinutes() > 0) ? config.resetTokenExpiryMinutes() : 30;
        user.resetToken = token;
        user.resetTokenExpiry = LocalDateTime.now().plusMinutes(expiryMinutes);
        user.persist();
        LOG.infof("Token de réinitialisation généré pour : %s (expiration dans %d min)", user.email, expiryMinutes);
        mailer.sendResetEmail(user.email, user.name, token);
    }

    @Transactional
    public void confirmPasswordReset(PasswordResetConfirm request) {
        User user = User.findByResetToken(request.token());
        if (user == null) {
            LOG.warnf("Tentative de réinitialisation avec un token invalide");
            throw new NotFoundException("Token invalide");
        }
        if (user.resetTokenExpiry == null || user.resetTokenExpiry.isBefore(LocalDateTime.now())) {
            LOG.warnf("Token de réinitialisation expiré pour : %s", user.email);
            throw new BadRequestException("Token expiré");
        }
        user.passwordHash = PasswordUtil.hash(request.newPassword());
        user.resetToken = null;
        user.resetTokenExpiry = null;
        user.persist();
        LOG.infof("Mot de passe réinitialisé avec succès pour : %s", user.email);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = User.findByEmail(email);
        if (user == null) throw new NotFoundException("Utilisateur non trouvé");
        if (!PasswordUtil.verify(request.currentPassword(), user.passwordHash)) {
            LOG.warnf("Changement de mot de passe refusé (mot de passe actuel incorrect) pour : %s", email);
            throw new BadRequestException("Mot de passe actuel incorrect");
        }
        user.passwordHash = PasswordUtil.hash(request.newPassword());
        user.persist();
        LOG.infof("Mot de passe changé avec succès pour : %s", email);
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
