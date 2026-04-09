package org.santalina.diving.unit;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.santalina.diving.domain.User;
import org.santalina.diving.domain.UserRole;
import org.santalina.diving.dto.UserDto.UpdateProfileRequest;
import org.santalina.diving.service.UserService;

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
                        new UpdateProfileRequest("inexistant@test.com", "Nouveau", "Nom", "+33600000000", null)));
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
    void deleteUser_shouldThrowNotFoundException_whenUserDoesNotExist() {
        assertThrows(NotFoundException.class,
                () -> userService.deleteUser(99999L));
    }
}
