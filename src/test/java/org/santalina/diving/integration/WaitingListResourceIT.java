package org.santalina.diving.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.domain.SlotDiver;
import org.santalina.diving.domain.User;
import org.santalina.diving.domain.UserRole;
import org.santalina.diving.domain.WaitingListEntry;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Tests d'intégration pour la liste d'attente (WaitingListResource).
 *
 * Stratégie :
 *   - Chaque test crée / nettoie ses propres données via des méthodes @Transactional.
 *   - On teste les accès autorisés ET refusés.
 */
@QuarkusTest
class WaitingListResourceIT {

    // ── helpers pour créer les fixtures ────────────────────────────────────────

    @Transactional
    DiveSlot createSlot(boolean registrationOpen) {
        User creator = new User();
        creator.email        = "dp_wl_test_" + System.nanoTime() + "@test.com";
        creator.firstName    = "DP";
        creator.lastName     = "TEST";
        creator.passwordHash = "x";
        creator.role         = UserRole.DIVE_DIRECTOR;
        creator.roles        = java.util.Set.of(UserRole.DIVE_DIRECTOR);
        creator.persist();

        DiveSlot slot = new DiveSlot();
        slot.slotDate         = LocalDate.of(2099, 8, 1);
        slot.startTime        = LocalTime.of(9, 0);
        slot.endTime          = LocalTime.of(12, 0);
        slot.diverCount       = 8;
        slot.createdBy        = creator;
        slot.registrationOpen = registrationOpen;
        slot.persist();

        // Ajouter un directeur de plongée (slot_diver) sur le créneau
        SlotDiver dp = new SlotDiver();
        dp.slot       = slot;
        dp.firstName  = "DP";
        dp.lastName   = "ASSIGNED";
        dp.level      = "MF1";
        dp.email      = creator.email;
        dp.isDirector = true;
        dp.persist();

        return slot;
    }

    @Transactional
    WaitingListEntry createEntry(Long slotId) {
        DiveSlot slot = DiveSlot.findById(slotId);
        WaitingListEntry entry = new WaitingListEntry();
        entry.slot          = slot;
        entry.firstName     = "Alice";
        entry.lastName      = "MARTIN";
        entry.email         = "alice_" + System.nanoTime() + "@test.com";
        entry.level         = "N2";
        entry.numberOfDives = 30;
        entry.lastDiveDate  = LocalDate.of(2025, 1, 15);
        entry.persist();
        return entry;
    }

    @Transactional
    void cleanup(Long slotId) {
        WaitingListEntry.delete("slot.id", slotId);
        SlotDiver.delete("slot.id", slotId);
        DiveSlot slot = DiveSlot.findById(slotId);
        if (slot != null) {
            User creator = slot.createdBy;
            slot.delete();
            if (creator != null) creator.delete();
        }
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @TestSecurity(user = "diver@test.com", roles = {"DIVER"})
    void register_shouldReturn400_whenNoDP() {
        // Créneau sans slot_diver directeur
        DiveSlot slot = new DiveSlot();
        try {
            createSlotWithoutDP(slot);
            given()
                .contentType(ContentType.JSON)
                .body("""
                    {"firstName":"Bob","lastName":"DUPONT",
                     "email":"bob@test.com","emailConfirm":"bob@test.com",
                     "level":"N1","numberOfDives":5,
                     "lastDiveDate":"2025-03-01"}
                    """)
                .when().post("/api/slots/{slotId}/waiting-list", slot.id)
                .then()
                .statusCode(400);
        } finally {
            cleanupSlotOnly(slot.id);
        }
    }

    @Test
    @TestSecurity(user = "diver@test.com", roles = {"DIVER"})
    void register_shouldReturn400_whenRegistrationClosed() {
        DiveSlot slot = createSlot(false); // registrationOpen = false
        try {
            given()
                .contentType(ContentType.JSON)
                .body("""
                    {"firstName":"Bob","lastName":"DUPONT",
                     "email":"bob@test.com","emailConfirm":"bob@test.com",
                     "level":"N1","numberOfDives":5,
                     "lastDiveDate":"2025-03-01"}
                    """)
                .when().post("/api/slots/{slotId}/waiting-list", slot.id)
                .then()
                .statusCode(400);
        } finally {
            cleanup(slot.id);
        }
    }

    @Test
    @TestSecurity(user = "diver@test.com", roles = {"DIVER"})
    void register_shouldReturn400_whenEmailMismatch() {
        DiveSlot slot = createSlot(true);
        try {
            given()
                .contentType(ContentType.JSON)
                .body("""
                    {"firstName":"Bob","lastName":"DUPONT",
                     "email":"bob@test.com","emailConfirm":"different@test.com",
                     "level":"N2","numberOfDives":10,
                     "lastDiveDate":"2025-03-01"}
                    """)
                .when().post("/api/slots/{slotId}/waiting-list", slot.id)
                .then()
                .statusCode(400);
        } finally {
            cleanup(slot.id);
        }
    }

    @Test
    @TestSecurity(user = "diver@test.com", roles = {"DIVER"})
    void register_shouldReturn201_whenRegistrationOpen() {
        DiveSlot slot = createSlot(true);
        try {
            given()
                .contentType(ContentType.JSON)
                .body("""
                    {"firstName":"Bob","lastName":"DUPONT",
                     "email":"bob_wl@test.com","emailConfirm":"bob_wl@test.com",
                     "level":"N2","numberOfDives":20,
                     "lastDiveDate":"2025-03-15"}
                    """)
                .when().post("/api/slots/{slotId}/waiting-list", slot.id)
                .then()
                .statusCode(201)
                .body("email", equalTo("bob_wl@test.com"))
                .body("level", equalTo("N2"));
        } finally {
            cleanup(slot.id);
        }
    }

    @Test
    void register_shouldReturn401_whenNotAuthenticated() {
        given()
            .contentType(ContentType.JSON)
            .body("{}")
            .when().post("/api/slots/1/waiting-list")
            .then()
            .statusCode(401);
    }

    @Test
    @TestSecurity(user = "unknown@test.com", roles = {"DIVER"})
    void getWaitingList_shouldReturn403_forDiver() {
        DiveSlot slot = createSlot(true);
        try {
            given()
                .when().get("/api/slots/{slotId}/waiting-list", slot.id)
                .then()
                .statusCode(403);
        } finally {
            cleanup(slot.id);
        }
    }

    @Test
    @TestSecurity(user = "dp_test@test.com", roles = {"DIVE_DIRECTOR"})
    void getWaitingList_shouldReturn403_forWrongDP() {
        // DP qui n'est pas propriétaire du créneau
        DiveSlot slot = createSlot(true);
        try {
            given()
                .when().get("/api/slots/{slotId}/waiting-list", slot.id)
                .then()
                .statusCode(403);
        } finally {
            cleanup(slot.id);
        }
    }

    @Test
    @TestSecurity(user = "admin@test.com", roles = {"ADMIN"})
    void getWaitingList_shouldReturn200_forAdmin() {
        DiveSlot slot = createSlot(true);
        try {
            given()
                .when().get("/api/slots/{slotId}/waiting-list", slot.id)
                .then()
                .statusCode(200)
                .body("$", instanceOf(java.util.List.class));
        } finally {
            cleanup(slot.id);
        }
    }

    @Test
    @TestSecurity(user = "admin@test.com", roles = {"ADMIN"})
    void approve_shouldReturn200_andTransferToDivers() {
        DiveSlot slot = createSlot(true);
        try {
            WaitingListEntry entry = createEntry(slot.id);

            given()
                .contentType(ContentType.JSON)
                .when().post("/api/slots/{slotId}/waiting-list/{entryId}/approve",
                        slot.id, entry.id)
                .then()
                .statusCode(200)
                .body("firstName", equalTo("Alice"))
                .body("isDirector", equalTo(false));
        } finally {
            cleanup(slot.id);
        }
    }

    @Test
    @TestSecurity(user = "admin@test.com", roles = {"ADMIN"})
    void cancel_shouldReturn204_forAdmin() {
        DiveSlot slot = createSlot(true);
        try {
            WaitingListEntry entry = createEntry(slot.id);

            given()
                .when().delete("/api/slots/{slotId}/waiting-list/{entryId}",
                        slot.id, entry.id)
                .then()
                .statusCode(204);
        } finally {
            cleanup(slot.id);
        }
    }

    @Test
    @TestSecurity(user = "unknown_diver@test.com", roles = {"DIVER"})
    void cancel_shouldReturn403_whenNotOwnerOrDP() {
        DiveSlot slot = createSlot(true);
        try {
            WaitingListEntry entry = createEntry(slot.id);

            given()
                .when().delete("/api/slots/{slotId}/waiting-list/{entryId}",
                        slot.id, entry.id)
                .then()
                .statusCode(403);
        } finally {
            cleanup(slot.id);
        }
    }

    // ── helpers secondaires ────────────────────────────────────────────────────

    @Transactional
    void createSlotWithoutDP(DiveSlot out) {
        DiveSlot slot = new DiveSlot();
        slot.slotDate         = LocalDate.of(2099, 9, 1);
        slot.startTime        = LocalTime.of(9, 0);
        slot.endTime          = LocalTime.of(12, 0);
        slot.diverCount       = 6;
        slot.registrationOpen = false;
        slot.persist();
        out.id = slot.id;
    }

    @Transactional
    void cleanupSlotOnly(Long slotId) {
        if (slotId == null) return;
        DiveSlot slot = DiveSlot.findById(slotId);
        if (slot != null) slot.delete();
    }
}
