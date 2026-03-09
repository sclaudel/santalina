package org.santalina.diving.security;

import org.santalina.diving.config.DivingConfig;
import org.santalina.diving.domain.User;
import org.santalina.diving.domain.UserRole;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class JwtUtil {

    @Inject
    DivingConfig config;

    public String generateToken(User user) {
        int expiryHours = (config.jwtExpiryHours() > 0) ? config.jwtExpiryHours() : 24;

        // Groupes = tous les rôles de l'utilisateur (String)
        Set<String> groups = (user.roles != null && !user.roles.isEmpty())
                ? user.roles.stream().map(UserRole::name).collect(Collectors.toSet())
                : Set.of(user.role.name());

        // Rôle principal pour le claim "role" (affichage frontend)
        String primaryRole = user.primaryRole().name();

        return Jwt.issuer("santalina-app")
                .subject(user.email)
                .groups(groups)
                .claim("userId", user.id)
                .claim("name", user.name)
                .claim("role", primaryRole)
                .expiresIn(Duration.ofHours(expiryHours))
                .sign();
    }
}
