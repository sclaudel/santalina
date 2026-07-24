package org.santalina.diving.unit;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.santalina.diving.domain.User;
import org.santalina.diving.domain.UserRole;
import org.santalina.diving.dto.UserDto.UpdateProfileRequest;
import org.santalina.diving.service.UserService;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires de UserService.
 * La base H2 in-memory est utilisée (profil test).
 */
@QuarkusTest
class UserServiceTest {

    @Inject
    UserService userService;

    @Test
    void getProfile_shouldThrowNotFoundException_whenUserDoesNotExist() {
        assertThrows(NotFoundException.class,
                () -> userService.getProfile("inexistant@test.com"));
    }

    @Test
    void updateProfile_shouldThrowNotFoundException_whenUserDoesNotExist() {
        assertThrows(NotFoundException.class,
                () -> userService.updateProfile("inexistant@test.com",
                        new UpdateProfileRequest("Nouveau", "Nom", "+33600000000", null, null)));
    }

    @Test
    void searchUsers_shouldReturnEmpty_whenQueryIsBlank() {
        var result = userService.searchUsers("   ");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void searchUsers_shouldReturnEmpty_whenQueryIsNull() {
        var result = userService.searchUsers(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @Transactional
    void searchUsers_shouldIgnoreAccents_whenQueryHasNoAccent() {
        User user = new User();
        user.email = "elodie.martin@example.com";
        user.firstName = "Élodie";
        user.lastName = "Martin";
        user.passwordHash = "hash";
        user.roles = Set.of(UserRole.DIVER);
        user.role = UserRole.DIVER;
        user.activated = true;
        user.persist();

        var result = userService.searchUsers("elodie");

        assertTrue(result.stream().anyMatch(found -> "elodie.martin@example.com".equals(found.email())));
    }

    @Test
    @Transactional
    void searchUsers_shouldFindAdèle_whenQueryIsAdèle() {
        User user = new User();
        user.email = "adele.durand@example.com";
        user.firstName = "Adèle";
        user.lastName = "Durand";
        user.passwordHash = "hash";
        user.roles = Set.of(UserRole.DIVER);
        user.role = UserRole.DIVER;
        user.activated = true;
        user.persist();

        var result = userService.searchUsers("Adèle");

        assertTrue(result.stream().anyMatch(found -> "adele.durand@example.com".equals(found.email())));
    }

    @Test
    void deleteUser_shouldThrowNotFoundException_whenUserDoesNotExist() {
        assertThrows(NotFoundException.class,
                () -> userService.deleteUser(99999L));
    }
}
