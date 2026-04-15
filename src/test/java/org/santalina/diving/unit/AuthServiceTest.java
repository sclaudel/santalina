package org.santalina.diving.unit;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.santalina.diving.domain.User;
import org.santalina.diving.domain.UserRole;
import org.santalina.diving.dto.AuthDto.ChangePasswordRequest;
import org.santalina.diving.dto.AuthDto.LoginRequest;
import org.santalina.diving.dto.AuthDto.LoginResponse;
import org.santalina.diving.dto.AuthDto.RegisterRequest;
import org.santalina.diving.mail.PasswordResetMailer;
import org.santalina.diving.security.JwtUtil;
import org.santalina.diving.security.PasswordUtil;
import org.santalina.diving.service.AuthService;
import org.santalina.diving.service.ConfigService;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de AuthService — les dépendances Panache sont mockées via @InjectMock.
 * Les méthodes qui accèdent à la BD (findByEmail, persist) sont simulées
 * en passant User directement (les méthodes statiques Panache sont gérées par Quarkus Mockito).
 */
@QuarkusTest
class AuthServiceTest {

    @Inject
    AuthService authService;

    @InjectMock
    ConfigService configService;

    @InjectMock
    JwtUtil jwtUtil;

    @InjectMock
    PasswordResetMailer mailer;

    @BeforeEach
    void setup() {
        when(configService.isSelfRegistration()).thenReturn(true);
        when(jwtUtil.generateToken(any())).thenReturn("mock-jwt-token");
    }

    @Test
    void changePassword_shouldThrowNotFound_whenUserDoesNotExist() {
        // User.findByEmail est mockable avec Quarkus Mockito via l'annotation @QuarkusTest
        // Ce test vérifie que NotFoundException est levée si l'utilisateur n'existe pas
        // (le mock retourne null par défaut pour les méthodes de User)
        assertThrows(NotFoundException.class,
                () -> authService.changePassword("inconnu@test.com",
                        new ChangePasswordRequest("old", "newPwd123")));
    }

    @Test
    void login_shouldThrow_whenUserNotFound() {
        assertThrows(Exception.class,
                () -> authService.login(new LoginRequest("inexistant@test.com", "pwd")));
    }
}