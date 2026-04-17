package org.santalina.diving.unit;

import org.junit.jupiter.api.Test;
import org.santalina.diving.domain.User;
import org.santalina.diving.mail.DpOrganizerMailer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires de DpOrganizerMailer (méthodes statiques utilitaires).
 */
class DpOrganizerMailerTest {

    private static User user(String firstName, String lastName, String email) {
        User u = new User();
        u.firstName = firstName;
        u.lastName  = lastName;
        u.email     = email;
        return u;
    }

    @Test
    void buildFromAddress_shouldReturnFormattedAddress() {
        User dp = user("Jean", "DUPONT", "jean.dupont@gmail.com");
        String from = DpOrganizerMailer.buildFromAddress(dp, dp.email);
        assertEquals("\"Jean DUPONT\" <jean.dupont@gmail.com>", from);
    }

    @Test
    void buildFromAddress_shouldEscapeDoubleQuotesInName() {
        User dp = user("O\"Brien", "Test", "test@example.com");
        String from = DpOrganizerMailer.buildFromAddress(dp, dp.email);
        assertEquals("\"O\\\"Brien Test\" <test@example.com>", from);
    }

    @Test
    void buildFromAddress_shouldReturnEmailOnly_whenNoName() {
        User dp = user("", "", "noemail@example.com");
        String from = DpOrganizerMailer.buildFromAddress(dp, dp.email);
        assertEquals("noemail@example.com", from);
    }

    @Test
    void buildFromAddress_shouldReturnNull_whenEmailNull() {
        User dp = user("Jean", "DUPONT", null);
        assertNull(DpOrganizerMailer.buildFromAddress(dp, null));
    }

    @Test
    void buildFromAddress_shouldReturnNull_whenEmailBlank() {
        assertNull(DpOrganizerMailer.buildFromAddress(null, "  "));
    }

    @Test
    void buildFromAddress_shouldReturnEmail_whenDpNull() {
        String from = DpOrganizerMailer.buildFromAddress(null, "solo@example.com");
        assertEquals("solo@example.com", from);
    }
}
