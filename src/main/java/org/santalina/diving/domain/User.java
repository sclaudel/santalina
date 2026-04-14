package org.santalina.diving.domain;


import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true)
    public String email;

    @Column(name = "password_hash")
    public String passwordHash;

    @Column(name = "first_name", nullable = false)
    public String firstName;

    @Column(name = "last_name", nullable = false)
    public String lastName;

    @Column(nullable = false)
    public boolean activated = true;

    @Column(name = "activation_token")
    public String activationToken;

    @Column(name = "activation_token_expiry")
    public LocalDateTime activationTokenExpiry;

    @Column
    public String phone;

    @Column(name = "license_number")
    public String licenseNumber;

    @Column
    public String club;

    /** Rôle principal (compatibilité ascendante) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public UserRole role = UserRole.DIVER;

    /** Ensemble des rôles — source de vérité pour le JWT et les droits */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    public Set<UserRole> roles = new HashSet<>();

    @Column(name = "consent_given", nullable = false)
    public boolean consentGiven = false;

    @Column(name = "consent_date")
    public LocalDateTime consentDate;

    @Column(name = "reset_token")
    public String resetToken;

    @Column(name = "reset_token_expiry")
    public LocalDateTime resetTokenExpiry;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt = LocalDateTime.now();

    // ---- Préférences de notifications e-mail ----

    @Column(name = "notif_on_registration", nullable = false)
    public boolean notifOnRegistration = true;

    @Column(name = "notif_on_approved", nullable = false)
    public boolean notifOnApproved = true;

    @Column(name = "notif_on_cancelled", nullable = false)
    public boolean notifOnCancelled = true;

    @Column(name = "notif_on_moved_to_waitlist", nullable = false)
    public boolean notifOnMovedToWaitlist = true;

    /** Recevoir les notifications d'inscription sur mes créneaux (en tant que DP/créateur) */
    @Column(name = "notif_on_dp_registration", nullable = false)
    public boolean notifOnDpRegistration = true;

    /** Recevoir les notifications d'inscription sur mes créneaux (en tant que créateur uniquement) */
    @Column(name = "notif_on_creator_registration", nullable = false)
    public boolean notifOnCreatorRegistration = false;

    /** Recevoir le rappel de fiche de sécurité (en tant que DP assigné sur un créneau) */
    @Column(name = "notif_on_safety_reminder", nullable = false)
    public boolean notifOnSafetyReminder = true;

    // ---- helpers ----

    /** Retourne le nom complet (prénom + nom). */
    public String fullName() {
        return (firstName + " " + lastName).trim();
    }

    public boolean hasRole(UserRole r) {
        return roles != null && roles.contains(r);
    }

    /** Retourne le rôle principal (le plus élevé) */
    public UserRole primaryRole() {
        if (roles == null || roles.isEmpty()) return role;
        if (roles.contains(UserRole.ADMIN))         return UserRole.ADMIN;
        if (roles.contains(UserRole.DIVE_DIRECTOR)) return UserRole.DIVE_DIRECTOR;
        return UserRole.DIVER;
    }

    // ---- Panache finders ----

    public static User findByEmail(String email) {
        if (email == null || email.isBlank()) return null;
        return find("lower(email) = lower(?1)", email.trim()).firstResult();
    }

    public static long countAdmins() {
        return count("role", UserRole.ADMIN);
    }

    public static User findByResetToken(String token) {
        return find("resetToken", token).firstResult();
    }

    public static User findByActivationToken(String token) {
        return find("activationToken", token).firstResult();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
