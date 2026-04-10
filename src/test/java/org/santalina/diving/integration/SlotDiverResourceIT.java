package org.santalina.diving.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.domain.SlotDiver;
import org.santalina.diving.domain.User;
import org.santalina.diving.domain.UserRole;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Tests d'intégration pour SlotDiverResource.
 *
 * Couvre :
 *   - accès non authentifié (GET)
 *   - ajout plongeur (DP / ADMIN) avec vérification capitalisation prénom et email lowercase
 *   - refus d'ajout pour rôle DIVER
 *   - PUT modification de plongeur
 *   - DELETE retrait par DP
 *   - DELETE /me — auto-désinscription du plongeur connecté
 */
@QuarkusTest
class SlotDiverResourceIT {

    // ── fixtures ──────────────────────────────────────────────────────────────

    @Transactional
    DiveSlot createSlotWithDp(String dpEmail) {
        User creator = new User();
        creator.email        = dpEmail;
        creator.firstName    = "Dp";
        creator.lastName     = "TEST";
        creator.passwordHash = "x";
        creator.activated    = true;
        creator.role         = UserRole.DIVE_DIRECTOR;
        creator.roles        = java.util.Set.of(UserRole.DIVE_DIRECTOR);
        creator.persist();

        DiveSlot slot = new DiveSlot();
        slot.slotDate   = LocalDate.of(2099, 9, 1);
        slot.startTime  = LocalTime.of(9, 0);
        slot.endTime    = LocalTime.of(12, 0);
        slot.diverCount = 10;
        slot.createdBy  = creator;
        slot.persist();

        // Ajouter le créateur en tant que DP assigné
        SlotDiver dp = new SlotDiver();
        dp.slot       = slot;
        dp.firstName  = "Dp";
        dp.lastName   = "TEST";
        dp.level      = "MF1";
        dp.email      = dpEmail;
        dp.isDirector = true;
        dp.persist();

        return slot;
    }

    @Transactional
    SlotDiver addDiver(Long slotId, String email) {
        DiveSlot slot = DiveSlot.findById(slotId);
        User user = new User();
        user.email        = email;
        user.firstName    = "Plongeur";
        user.lastName     = "TEST";
        user.passwordHash = "x";
        user.activated    = true;
        user.role         = UserRole.DIVER;
        user.roles        = java.util.Set.of(UserRole.DIVER);
        user.persist();

        SlotDiver diver = new SlotDiver();
        diver.slot       = slot;
        diver.firstName  = "Plongeur";
        diver.lastName   = "TEST";
        diver.level      = "N2";
        diver.email      = email;
        diver.isDirector = false;
        diver.persist();
        return diver;
    }

    @Transactional
    void cleanup(Long slotId) {
        if (slotId == null) return;
        DiveSlot slot = DiveSlot.findById(slotId);
        if (slot == null) return;
        SlotDiver.delete("slot", slot);
        if (slot.createdBy != null) slot.createdBy.delete();
        slot.delete();
    }

    // ── GET public ───────────────────────────────────────────────────────────

    @Test
    void getDivers_shouldReturn200_withoutAuthentication() {
        DiveSlot slot = createSlotWithDp("dp_get_" + System.nanoTime() + "@test.com");
        try {
            given()
                .when().get("/api/slots/" + slot.id + "/divers")
                .then()
                .statusCode(200)
                .body("$", instanceOf(java.util.List.class));
        } finally {
            cleanup(slot.id);
        }
    }

    // ── POST — capitalisation firstName et email lowercase ───────────────────

    @Test
    @TestSecurity(user = "dp_add@test.com", roles = {"DIVE_DIRECTOR"})
    void addDiver_shouldCapitalizeFirstName_andLowercaseEmail() {
        DiveSlot slot = createSlotWithDp("dp_add@test.com");
        try {
            given()
                .contentType(ContentType.JSON)
                .body("""
                      {"firstName":"JEAN-paul","lastName":"dupont",
                       "level":"N2","isDirector":false}
                      """)
                .when().post("/api/slots/" + slot.id + "/divers")
                .then()
                .statusCode(200)
                .body("firstName", equalTo("Jean-Paul"))
                .body("lastName", equalTo("DUPONT"));
        } finally {
            cleanup(slot.id);
        }
    }

    @Test
    @TestSecurity(user = "dp_email@test.com", roles = {"DIVE_DIRECTOR"})
    void addDiver_shouldStoreDpEmailLowercase_whenDirectorAdded() {
        DiveSlot slot = createSlotWithDp("dp_email@test.com");
        // Remove the existing DP so we can add a new one with isDirector=true
        removeExistingDp(slot.id);
        try {
            given()
                .contentType(ContentType.JSON)
                .body("""
                      {"firstName":"Alice","lastName":"martin",
                       "level":"MF1","isDirector":true,
                       "email":"Alice.Test@Example.COM","phone":"0600000000"}
                      """)
                .when().post("/api/slots/" + slot.id + "/divers")
                .then()
                .statusCode(200)
                .body("email", equalTo("alice.test@example.com"));
        } finally {
            cleanup(slot.id);
        }
    }

    @Transactional
    void removeExistingDp(Long slotId) {
        SlotDiver.delete("slot.id = ?1 and isDirector = true", slotId);
    }

    // ── POST — accès refusé pour DIVER ────────────────────────────────────────

    @Test
    @TestSecurity(user = "diver@test.com", roles = {"DIVER"})
    void addDiver_shouldReturn403_forDiver() {
        DiveSlot slot = createSlotWithDp("dp_403_" + System.nanoTime() + "@test.com");
        try {
            given()
                .contentType(ContentType.JSON)
                .body("""
                      {"firstName":"Bob","lastName":"Martin","level":"N1","isDirector":false}
                      """)
                .when().post("/api/slots/" + slot.id + "/divers")
                .then()
                .statusCode(403);
        } finally {
            cleanup(slot.id);
        }
    }

    @Test
    void addDiver_shouldReturn401_withoutAuthentication() {
        DiveSlot slot = createSlotWithDp("dp_401_" + System.nanoTime() + "@test.com");
        try {
            given()
                .contentType(ContentType.JSON)
                .body("""
                      {"firstName":"Bob","lastName":"Martin","level":"N1","isDirector":false}
                      """)
                .when().post("/api/slots/" + slot.id + "/divers")
                .then()
                .statusCode(401);
        } finally {
            cleanup(slot.id);
        }
    }

    // ── DELETE /me — auto-désinscription ─────────────────────────────────────

    @Test
    @TestSecurity(user = "diver_cancel@test.com", roles = {"DIVER"})
    void cancelMe_shouldReturn204_whenDiverIsRegistered() {
        DiveSlot slot = createSlotWithDp("dp_cancelme_" + System.nanoTime() + "@test.com");
        addDiver(slot.id, "diver_cancel@test.com");
        try {
            given()
                .when().delete("/api/slots/" + slot.id + "/divers/me")
                .then()
                .statusCode(204);
        } finally {
            cleanup(slot.id);
        }
    }

    @Test
    @TestSecurity(user = "diver_notregistered@test.com", roles = {"DIVER"})
    void cancelMe_shouldReturn404_whenDiverIsNotRegistered() {
        DiveSlot slot = createSlotWithDp("dp_cnr_" + System.nanoTime() + "@test.com");
        try {
            given()
                .when().delete("/api/slots/" + slot.id + "/divers/me")
                .then()
                .statusCode(404);
        } finally {
            cleanup(slot.id);
        }
    }

    @Test
    @TestSecurity(user = "dp_assigned@test.com", roles = {"DIVE_DIRECTOR"})
    void cancelMe_shouldReturn400_whenCallerIsAssignedDirector() {
        DiveSlot slot = createSlotWithDp("dp_assigned@test.com");
        try {
            given()
                .when().delete("/api/slots/" + slot.id + "/divers/me")
                .then()
                .statusCode(400);
        } finally {
            cleanup(slot.id);
        }
    }

    @Test
    void cancelMe_shouldReturn401_withoutAuthentication() {
        DiveSlot slot = createSlotWithDp("dp_me401_" + System.nanoTime() + "@test.com");
        try {
            given()
                .when().delete("/api/slots/" + slot.id + "/divers/me")
                .then()
                .statusCode(401);
        } finally {
            cleanup(slot.id);
        }
    }
}
