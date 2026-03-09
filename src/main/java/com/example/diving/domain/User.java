package com.example.diving.domain;


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

    @Column(name = "password_hash", nullable = false)
    public String passwordHash;

    @Column(nullable = false)
    public String name;

    @Column
    public String phone;

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

    @Column(name = "reset_token")
    public String resetToken;

    @Column(name = "reset_token_expiry")
    public LocalDateTime resetTokenExpiry;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt = LocalDateTime.now();

    // ---- helpers ----

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
        return find("email", email).firstResult();
    }

    public static long countAdmins() {
        return count("role", UserRole.ADMIN);
    }

    public static User findByResetToken(String token) {
        return find("resetToken", token).firstResult();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
