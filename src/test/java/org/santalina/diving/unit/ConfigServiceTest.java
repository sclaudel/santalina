package org.santalina.diving.unit;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.santalina.diving.service.ConfigService;

import java.time.LocalDate;
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

    @Test
    void isMaintenanceMode_shouldReturnBoolean() {
        // Vérifie que la méthode ne lève pas d'exception (valeur dépend de l'état de la base)
        assertDoesNotThrow(() -> configService.isMaintenanceMode());
    }

    @Test
    @TestTransaction
    void updateMaintenanceMode_shouldToggleAndReturnUpdatedConfig() {
        boolean initial = configService.isMaintenanceMode();

        // Activer la maintenance
        configService.updateMaintenanceMode(true);
        assertTrue(configService.isMaintenanceMode(),
                "Le mode maintenance doit être actif après activation");

        // Désactiver la maintenance
        configService.updateMaintenanceMode(false);
        assertFalse(configService.isMaintenanceMode(),
                "Le mode maintenance doit être inactif après désactivation");

        // Restaurer l'état initial
        configService.updateMaintenanceMode(initial);
    }

    @Test
    void getConfig_shouldIncludeMaintenanceMode() {
        var config = configService.getConfig();
        // maintenanceMode est un booléen — juste vérifier qu'il est présent sans exception
        assertNotNull(config);
        // La valeur retournée doit être cohérente avec le getter direct
        assertEquals(configService.isMaintenanceMode(), config.maintenanceMode());
    }

    // ── Date d'activation du rappel fiche de sécurité ───────────────────────

    @Test
    @TestTransaction
    void updateNotifSettings_shouldAutoSetActivationDate_whenFirstEnabled() {
        // On s'assure qu'aucune date n'est définie avant le test
        configService.updateNotifSettings(true, true, true, true, true,
                false, 3, "corps du rappel", null);
        // Activer le rappel pour la première fois → la date doit être auto-définie à aujourd'hui
        var config = configService.updateNotifSettings(true, true, true, true, true,
                true, 3, "corps du rappel", null);

        assertNotNull(config.safetyReminderActivationDate(),
                "La date d'activation doit être définie dans la réponse");
        assertFalse(config.safetyReminderActivationDate().isBlank(),
                "La date d'activation ne doit pas être vide");
        assertEquals(LocalDate.now().toString(), config.safetyReminderActivationDate(),
                "La date d'activation doit correspondre à aujourd'hui");
    }

    @Test
    @TestTransaction
    void updateNotifSettings_shouldNotOverrideActivationDate_whenAlreadySet() {
        // Premier enable : date = aujourd'hui
        configService.updateNotifSettings(true, true, true, true, true,
                true, 3, "corps", null);
        String firstDate = configService.getConfig().safetyReminderActivationDate();
        assertNotNull(firstDate);

        // Deuxième appel avec enabled=true sans passer de date : la date ne doit pas changer
        var config = configService.updateNotifSettings(true, true, true, true, true,
                true, 5, "nouveau corps", null);

        assertEquals(firstDate, config.safetyReminderActivationDate(),
                "La date d'activation ne doit pas être réécrasée si elle est déjà définie");
    }

    @Test
    @TestTransaction
    void updateNotifSettings_shouldOverrideActivationDate_whenExplicitlyProvided() {
        // Premier enable
        configService.updateNotifSettings(true, true, true, true, true,
                true, 3, "corps", null);

        // Fournir une nouvelle date explicite
        String newDate = "2026-01-01";
        var config = configService.updateNotifSettings(true, true, true, true, true,
                true, 3, "corps", newDate);

        assertEquals(newDate, config.safetyReminderActivationDate(),
                "La date d'activation doit être mise à jour quand une valeur explicite est fournie");
    }
}
