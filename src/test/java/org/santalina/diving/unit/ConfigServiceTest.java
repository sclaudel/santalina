package org.santalina.diving.unit;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.santalina.diving.service.ConfigService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires de ConfigService avec la base H2 in-memory.
 */
@QuarkusTest
class ConfigServiceTest {

    @Inject
    ConfigService configService;

    @Test
    void getMaxDivers_shouldReturnPositiveValue() {
        int max = configService.getMaxDivers();
        assertTrue(max > 0, "La capacité maximale doit être positive");
    }

    @Test
    void getSlotMinHours_shouldBeAtLeastOne() {
        int min = configService.getSlotMinHours();
        assertTrue(min >= 1, "La durée minimale doit être au moins 1 heure");
    }

    @Test
    void getSlotMaxHours_shouldBeGreaterThanMin() {
        int min = configService.getSlotMinHours();
        int max = configService.getSlotMaxHours();
        assertTrue(max > min, "La durée maximale doit être supérieure à la durée minimale");
    }

    @Test
    void getSlotResolutionMinutes_shouldBePositiveMultipleOf5() {
        int resolution = configService.getSlotResolutionMinutes();
        assertTrue(resolution > 0 && 60 % resolution == 0,
                "La résolution doit être un diviseur de 60 (ex: 15, 30)");
    }

    @Test
    void getSiteName_shouldNotBeBlank() {
        String name = configService.getSiteName();
        assertNotNull(name);
        assertFalse(name.isBlank(), "Le nom du site ne doit pas être vide");
    }

    @Test
    void getSlotTypes_shouldReturnNonEmptyList() {
        List<String> types = configService.getSlotTypes();
        assertNotNull(types);
        assertFalse(types.isEmpty(), "La liste des types de créneaux ne doit pas être vide");
    }

    @Test
    void getConfig_shouldReturnConsistentValues() {
        var config = configService.getConfig();
        assertNotNull(config);
        assertEquals(configService.getMaxDivers(), config.maxDivers());
        assertEquals(configService.getSiteName(), config.siteName());
        assertEquals(configService.getSlotTypes(), config.slotTypes());
    }

    @Test
    void isPublicAccess_shouldReturnBoolean() {
        // vérifie simplement que la méthode ne lève pas d'exception
        assertDoesNotThrow(() -> configService.isPublicAccess());
    }

    @Test
    void isSelfRegistration_shouldReturnBoolean() {
        assertDoesNotThrow(() -> configService.isSelfRegistration());
    }
}
