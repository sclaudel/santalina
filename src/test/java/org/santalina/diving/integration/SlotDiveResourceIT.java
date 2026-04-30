package org.santalina.diving.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.domain.Palanquee;
import org.santalina.diving.domain.SlotDive;
import org.santalina.diving.domain.SlotDiver;
import org.santalina.diving.domain.User;
import org.santalina.diving.domain.UserRole;

import java.time.LocalDate;
import java.time.LocalTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Tests d'intégration pour SlotDiveResource.
 *
 * Couvre :
 *   - Liste des plongées (GET)
 *   - Création d'une plongée (POST)
 *   - Mise à jour (PATCH)
 *   - Suppression (DELETE — détache les palanquées)
 *   - Assignation d'une palanquée à une plongée (PUT /assign)
 *   - Accès refusé pour DIVER ou sans authentification
 */
@QuarkusTest
class SlotDiveResourceIT {

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
        slot.slotDate   = LocalDate.of(2099, 10, 1);
        slot.startTime  = LocalTime.of(8, 0);
        slot.endTime    = LocalTime.of(17, 0);
        slot.diverCount = 12;
        slot.createdBy  = creator;
        slot.persist();

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
    SlotDive createDive(Long slotId, String label) {
        DiveSlot slot = DiveSlot.findById(slotId);
        SlotDive dive = new SlotDive();
        dive.slot      = slot;
        dive.diveIndex = (int) SlotDive.count("slot.id", slotId) + 1;
        dive.label     = label;
        dive.startTime = LocalTime.of(9, 0);
        dive.endTime   = LocalTime.of(12, 0);
        dive.persist();
        return dive;
    }

    @Transactional
    Palanquee createPalanquee(Long slotId, String name) {
        DiveSlot slot = DiveSlot.findById(slotId);
        Palanquee p = new Palanquee();
        p.slot     = slot;
        p.name     = name;
        p.position = 0;
        p.persist();
        return p;
    }

    @Transactional
    void cleanup(Long slotId) {
        if (slotId == null) return;
        DiveSlot slot = DiveSlot.findById(slotId);
        if (slot == null) return;
        Palanquee.update("slotDive = null WHERE slot.id = ?1", slotId);
        SlotDiver.delete("slot", slot);
        SlotDive.delete("slot", slot);
        Palanquee.delete("slot", slot);
        if (slot.createdBy != null) slot.createdBy.delete();
        slot.delete();
    }

    // ── GET ───────────────────────────────────────────────────────────────────

    @Test
    @TestSecurity(user = "dp_list@test.com", roles = {"DIVE_DIRECTOR"})
    void listDives_shouldReturn200_andEmptyList_whenNone() {
        DiveSlot slot = createSlotWithDp("dp_list@test.com");
        try {
            given()
                .when().get("/api/slots/" + slot.id + "/dives")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
        } finally {
            cleanup(slot.id);
        }
    }

    @Test
    void listDives_shouldReturn401_withoutAuthentication() {
        DiveSlot slot = createSlotWithDp("dp_list_401_" + System.nanoTime() + "@test.com");
        try {
            given()
                .when().get("/api/slots/" + slot.id + "/dives")
                .then()
                .statusCode(401);
        } finally {
            cleanup(slot.id);
        }
    }

    @Test
    @TestSecurity(user = "diver_list@test.com", roles = {"DIVER"})
    void listDives_shouldReturn403_forDiver() {
        DiveSlot slot = createSlotWithDp("dp_list_diver_" + System.nanoTime() + "@test.com");
        try {
            given()
                .when().get("/api/slots/" + slot.id + "/dives")
                .then()
                .statusCode(403);
        } finally {
            cleanup(slot.id);
        }
    }

    // ── POST ──────────────────────────────────────────────────────────────────

    @Test
    @TestSecurity(user = "dp_create@test.com", roles = {"DIVE_DIRECTOR"})
    void createDive_shouldReturn201_withLabel() {
        DiveSlot slot = createSlotWithDp("dp_create@test.com");
        try {
            given()
                .contentType(ContentType.JSON)
                .body("""
                      {"label":"Plongée matin","startTime":"09:00","endTime":"12:00","depth":"20m","duration":"40'"}
                      """)
                .when().post("/api/slots/" + slot.id + "/dives")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("label", equalTo("Plongée matin"))
                .body("depth", equalTo("20m"))
                .body("duration", equalTo("40'"))
                .body("diveIndex", equalTo(1));
        } finally {
            cleanup(slot.id);
        }
    }

    @Test
    @TestSecurity(user = "dp_create_times@test.com", roles = {"DIVE_DIRECTOR"})
    void createDive_shouldPersistStartTimeAndEndTime() {
        DiveSlot slot = createSlotWithDp("dp_create_times@test.com");
        try {
            given()
                .contentType(ContentType.JSON)
                .body("""
                      {"label":"Matin","startTime":"09:00","endTime":"12:00"}
                      """)
                .when().post("/api/slots/" + slot.id + "/dives")
                .then()
                .statusCode(201)
                .body("startTime", equalTo("09:00:00"))
                .body("endTime", equalTo("12:00:00"));
        } finally {
            cleanup(slot.id);
        }
    }

    @Test
    @TestSecurity(user = "dp_create2@test.com", roles = {"DIVE_DIRECTOR"})
    void createDive_shouldAutoIncrementDiveIndex() {
        DiveSlot slot = createSlotWithDp("dp_create2@test.com");
        createDive(slot.id, "Première");
        try {
            given()
                .contentType(ContentType.JSON)
                .body("{\"label\":\"Deuxième\"}")
                .when().post("/api/slots/" + slot.id + "/dives")
                .then()
                .statusCode(201)
                .body("diveIndex", equalTo(2));
        } finally {
            cleanup(slot.id);
        }
    }

    // ── PATCH ─────────────────────────────────────────────────────────────────

    @Test
    @TestSecurity(user = "dp_patch@test.com", roles = {"DIVE_DIRECTOR"})
    void updateDive_shouldUpdateLabel_andReturnUpdated() {
        DiveSlot slot = createSlotWithDp("dp_patch@test.com");
        SlotDive dive = createDive(slot.id, "Matin");
        try {
            given()
                .contentType(ContentType.JSON)
                .body("""
                      {"label":"Matin modifié","depth":"30m"}
                      """)
                .when().patch("/api/slots/" + slot.id + "/dives/" + dive.id)
                .then()
                .statusCode(200)
                .body("label", equalTo("Matin modifié"))
                .body("depth", equalTo("30m"));
        } finally {
            cleanup(slot.id);
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Test
    @TestSecurity(user = "dp_delete@test.com", roles = {"DIVE_DIRECTOR"})
    void deleteDive_shouldReturn204_andDetachPalanquees() {
        DiveSlot slot = createSlotWithDp("dp_delete@test.com");
        SlotDive dive = createDive(slot.id, "A supprimer");
        Palanquee pal = createPalanquee(slot.id, "P1");
        // Assigner la palanquée à la plongée
        assignPalanqueeToDive(pal.id, dive.id);

        try {
            given()
                .when().delete("/api/slots/" + slot.id + "/dives/" + dive.id)
                .then()
                .statusCode(204);

            // Vérifier que la palanquée est bien détachée
            given()
                .when().get("/api/slots/" + slot.id + "/palanquees")
                .then()
                .statusCode(200)
                .body("[0].slotDiveId", nullValue());
        } finally {
            cleanup(slot.id);
        }
    }

    // ── PUT /assign ────────────────────────────────────────────────────────────

    @Test
    @TestSecurity(user = "dp_assign@test.com", roles = {"DIVE_DIRECTOR"})
    void assignPalanquee_shouldAssign_andUnassign() {
        DiveSlot slot = createSlotWithDp("dp_assign@test.com");
        SlotDive dive = createDive(slot.id, "Matin");
        Palanquee pal = createPalanquee(slot.id, "P1");

        try {
            // Assigner
            given()
                .contentType(ContentType.JSON)
                .body("{\"palanqueeId\":" + pal.id + ",\"slotDiveId\":" + dive.id + "}")
                .when().put("/api/slots/" + slot.id + "/dives/assign")
                .then()
                .statusCode(200);

            // Vérifier via GET
            given()
                .when().get("/api/slots/" + slot.id + "/palanquees")
                .then()
                .statusCode(200)
                .body("[0].slotDiveId", equalTo(dive.id.intValue()));

            // Désassigner
            given()
                .contentType(ContentType.JSON)
                .body("{\"palanqueeId\":" + pal.id + ",\"slotDiveId\":null}")
                .when().put("/api/slots/" + slot.id + "/dives/assign")
                .then()
                .statusCode(200);

            // Vérifier désassignation
            given()
                .when().get("/api/slots/" + slot.id + "/palanquees")
                .then()
                .statusCode(200)
                .body("[0].slotDiveId", nullValue());
        } finally {
            cleanup(slot.id);
        }
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    @Transactional
    void assignPalanqueeToDive(Long palanqueeId, Long diveId) {
        Palanquee p = Palanquee.findById(palanqueeId);
        SlotDive d  = SlotDive.findById(diveId);
        if (p != null && d != null) p.slotDive = d;
    }
}
