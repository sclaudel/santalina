package org.santalina.diving.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.domain.User;
import org.santalina.diving.domain.UserRole;
import org.santalina.diving.domain.WaitingListEntry;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Tests d'intégration pour BackupResource.
 *
 * Couvre :
 *   - accès non authentifié (401) et mauvais rôle (403)
 *   - export full : structure et présence des listes d'attente
 *   - export config-users : slots / waitingListEntries = null
 *   - import avec corps vide → 400
 *   - import complet (1 créneau + 1 liste d'attente) → waitingListRestored = 1
 *
 * ⚠️ Le test d'import (ordre 7) est destructif : il vide toute la base puis
 * importe un jeu minimal. Il est placé en dernier pour minimiser l'impact sur
 * les autres tests de la classe. Les valeurs de configuration ont toutes des
 * valeurs par défaut codées dans ConfigService, donc les autres tests
 * d'intégration restent valides même après la suppression des entrées DB.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BackupResourceIT {

    // ── Helpers de fixtures ───────────────────────────────────────────────────

    @Transactional
    DiveSlot createSlotWithWaitingListEntry(String suffix) {
        User creator = new User();
        creator.email        = "backup_dp_" + suffix + "@test.com";
        creator.firstName    = "Backup";
        creator.lastName     = "TEST";
        creator.passwordHash = "x";
        creator.activated    = true;
        creator.role         = UserRole.DIVE_DIRECTOR;
        creator.roles        = java.util.Set.of(UserRole.DIVE_DIRECTOR);
        creator.persist();

        DiveSlot slot = new DiveSlot();
        slot.slotDate   = LocalDate.of(2099, 12, 1);
        slot.startTime  = LocalTime.of(9, 0);
        slot.endTime    = LocalTime.of(12, 0);
        slot.diverCount = 8;
        slot.createdBy  = creator;
        slot.createdAt  = LocalDateTime.now();
        slot.updatedAt  = LocalDateTime.now();
        slot.persist();

        WaitingListEntry wl = new WaitingListEntry();
        wl.slot         = slot;
        wl.firstName    = "Attente";
        wl.lastName     = "TEST";
        wl.email        = "backup_wl_" + suffix + "@test.com";
        wl.level        = "N2";
        wl.registeredAt = LocalDateTime.now();
        wl.persist();

        return slot;
    }

    @Transactional
    void cleanup(Long slotId) {
        if (slotId == null) return;
        DiveSlot slot = DiveSlot.findById(slotId);
        if (slot == null) return;
        WaitingListEntry.delete("slot", slot);
        if (slot.createdBy != null) slot.createdBy.delete();
        slot.delete();
    }

    // ── Tests d'autorisation ──────────────────────────────────────────────────

    @Test
    @Order(1)
    void exportFull_shouldReturn401_withoutAuthentication() {
        given()
                .when().get("/api/admin/backup/export/full")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(2)
    @TestSecurity(user = "diver@test.com", roles = {"DIVER"})
    void exportFull_shouldReturn403_asDiver() {
        given()
                .when().get("/api/admin/backup/export/full")
                .then()
                .statusCode(403);
    }

    @Test
    @Order(3)
    @TestSecurity(user = "diver@test.com", roles = {"DIVER"})
    void importBackup_shouldReturn403_asDiver() {
        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when().post("/api/admin/backup/import")
                .then()
                .statusCode(403);
    }

    // ── Tests d'export ────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @TestSecurity(user = "admin@test.com", roles = {"ADMIN"})
    void exportFull_shouldReturn200_andContainWaitingListEntries() {
        String suffix = String.valueOf(System.nanoTime());
        DiveSlot slot = createSlotWithWaitingListEntry(suffix);
        try {
            given()
                    .when().get("/api/admin/backup/export/full")
                    .then()
                    .statusCode(200)
                    .body("version", notNullValue())
                    .body("type", equalTo("full"))
                    .body("exportedAt", notNullValue())
                    .body("config", notNullValue())
                    .body("users", notNullValue())
                    .body("slots", notNullValue())
                    .body("divers", notNullValue())
                    .body("palanquees", notNullValue())
                    .body("waitingListEntries", not(empty()));
        } finally {
            cleanup(slot.id);
        }
    }

    @Test
    @Order(5)
    @TestSecurity(user = "admin@test.com", roles = {"ADMIN"})
    void exportConfigUsers_shouldReturn200_andNotContainSlots() {
        given()
                .when().get("/api/admin/backup/export/config-users")
                .then()
                .statusCode(200)
                .body("version", notNullValue())
                .body("type", equalTo("config-users"))
                .body("config", notNullValue())
                .body("users", notNullValue())
                .body("slots", nullValue())
                .body("divers", nullValue())
                .body("palanquees", nullValue())
                .body("waitingListEntries", nullValue());
    }

    // ── Tests d'import ────────────────────────────────────────────────────────

    @Test
    @Order(6)
    @TestSecurity(user = "admin@test.com", roles = {"ADMIN"})
    void importBackup_withNullBody_shouldReturn400() {
        given()
                .contentType(ContentType.JSON)
                .when().post("/api/admin/backup/import")
                .then()
                .statusCode(400)
                .body("success", equalTo(false));
    }

    /**
     * Test destructif : vide toute la base, importe un payload avec 1 créneau
     * et 1 liste d'attente, vérifie que waitingListRestored = 1.
     * Placé en dernier pour ne pas affecter les autres tests.
     */
    @Test
    @Order(7)
    @TestSecurity(user = "admin@test.com", roles = {"ADMIN"})
    void importBackup_withWaitingListEntries_shouldRestoreWaitingList() {
        String payload = """
                {
                    "version": "1.0",
                    "type": "full",
                    "exportedAt": "2099-01-01T00:00:00",
                    "config": [{"key": "site.name", "value": "Test Import"}],
                    "users": [],
                    "slots": [
                        {
                            "id": 9001,
                            "slotDate": "2099-12-25",
                            "startTime": "09:00:00",
                            "endTime": "12:00:00",
                            "diverCount": 6,
                            "title": "Import Test Slot",
                            "notes": null,
                            "slotType": null,
                            "club": null,
                            "createdById": null,
                            "createdAt": "2099-01-01T00:00:00",
                            "registrationOpen": false,
                            "registrationOpensAt": null
                        }
                    ],
                    "divers": [],
                    "palanquees": [],
                    "waitingListEntries": [
                        {
                            "id": 1,
                            "slotId": 9001,
                            "firstName": "Jean",
                            "lastName": "DUPONT",
                            "email": "jean.dupont@test.com",
                            "level": "N2",
                            "numberOfDives": 10,
                            "lastDiveDate": null,
                            "preparedLevel": null,
                            "comment": "Test commentaire",
                            "registeredAt": "2099-01-01T00:00:00",
                            "medicalCertDate": "2099-01-01",
                            "licenseConfirmed": true,
                            "club": "Club Santalina"
                        }
                    ]
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post("/api/admin/backup/import")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("configRestored", equalTo(1))
                .body("slotsRestored", equalTo(1))
                .body("diversRestored", equalTo(0))
                .body("waitingListRestored", equalTo(1));
    }

    /**
     * Vérifie que l'import d'un utilisateur avec club fonctionne correctement.
     */
    @Test
    @Order(8)
    @TestSecurity(user = "admin@test.com", roles = {"ADMIN"})
    void importBackup_withUserWithClub_shouldRestoreClub() {
        String payload = """
                {
                    "version": "1.0",
                    "type": "config-users",
                    "exportedAt": "2099-01-01T00:00:00",
                    "config": [],
                    "users": [
                        {
                            "id": 9999,
                            "email": "club.import@test.com",
                            "passwordHash": "$2a$10$test",
                            "firstName": "Club",
                            "lastName": "IMPORT",
                            "phone": null,
                            "licenseNumber": null,
                            "club": "Club Santalina",
                            "activated": true,
                            "consentGiven": false,
                            "consentDate": null,
                            "roles": ["DIVER"],
                            "notifOnRegistration": true,
                            "notifOnApproved": true,
                            "notifOnCancelled": true,
                            "notifOnMovedToWaitlist": true,
                            "notifOnDpRegistration": true,
                            "notifOnCreatorRegistration": false,
                            "notifOnSafetyReminder": true
                        }
                    ],
                    "slots": null,
                    "divers": null,
                    "palanquees": null,
                    "waitingListEntries": null
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post("/api/admin/backup/import")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("usersRestored", equalTo(1));

        // Vérifier que le club est bien persisté
        org.junit.jupiter.api.Assertions.assertEquals("Club Santalina", findUserClub("club.import@test.com"));
    }

    @Transactional
    String findUserClub(String email) {
        User u = User.findByEmail(email);
        return u != null ? u.club : null;
    }
