package org.santalina.diving.unit;

import org.junit.jupiter.api.Test;
import org.santalina.diving.security.PasswordUtil;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    @Test
    void hash_shouldProduceDifferentHashForSamePassword() {
        String hash1 = PasswordUtil.hash("monMotDePasse");
        String hash2 = PasswordUtil.hash("monMotDePasse");
        assertNotEquals(hash1, hash2, "Deux hachages du même mot de passe doivent être différents (sel aléatoire)");
    }

    @Test
    void verify_shouldReturnTrueForCorrectPassword() {
        String password = "Santalina2026!";
        String hash = PasswordUtil.hash(password);
        assertTrue(PasswordUtil.verify(password, hash));
    }

    @Test
    void verify_shouldReturnFalseForWrongPassword() {
        String hash = PasswordUtil.hash("bonMotDePasse");
        assertFalse(PasswordUtil.verify("mauvaisMotDePasse", hash));
    }

    @Test
    void hash_shouldNotBeNull() {
        assertNotNull(PasswordUtil.hash("test"));
    }

    @Test
    void hash_shouldStartWithBCryptPrefix() {
        String hash = PasswordUtil.hash("test");
        assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$"),
                "Le hash doit être au format BCrypt");
    }
}
