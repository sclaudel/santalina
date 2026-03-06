package com.example.diving.security;

import com.example.diving.config.DivingConfig;
import com.example.diving.domain.User;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;
import java.util.Set;

@ApplicationScoped
public class JwtUtil {

    @Inject
    DivingConfig config;

    public String generateToken(User user) {
        int expiryHours = (config.jwtExpiryHours() > 0) ? config.jwtExpiryHours() : 24;
        return Jwt.issuer("lac-plongee-app")
                .subject(user.email)
                .groups(Set.of(user.role.name()))
                .claim("userId", user.id)
                .claim("name", user.name)
                .claim("role", user.role.name())
                .expiresIn(Duration.ofHours(expiryHours))
                .sign();
    }
}
