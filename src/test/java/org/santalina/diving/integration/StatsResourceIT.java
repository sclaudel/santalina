package org.santalina.diving.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.domain.SlotDiver;
import org.santalina.diving.domain.User;
import org.santalina.diving.domain.UserRole;

import java.time.LocalDate;
import java.time.LocalTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Tests d'intégration pour StatsResource.
 *
 * Couvre :
 *   - GET /api/stats (admin)
 *   - GET /api/stats/my (stats personnelles du DP)
 *   - Cas où le DP dirige un créneau qu'il n'a pas créé
 */
@QuarkusTest
class StatsResourceIT {

    // ── fixtures ──────────────────────────────────────────────────────────────

    @Transactional
    User createUser(String email, String firstName, String lastName, UserRole role) {
        User u = new User();
        u.email        = email;
        u.firstName    = firstName;
        u.lastName     = lastName;
        u.passwordHash = "x";
        u.activated    = true;
        u.role         = role;
        u.roles        = java.util.Set.of(role);
        u.persist();
        return u;
    }

    @Transactional
    DiveSlot createSlot(User creator, LocalDate date) {
        DiveSlot slot = new DiveSlot();
        slot.slotDate   = date;
        slot.startTime  = LocalTime.of(9, 0);
        slot.endTime    = LocalTime.of(17, 0);
        slot.diverCount = 10;
        slot.createdBy  = creator;
        slot.persist();
        return slot;
    }

    @Transactional
    SlotDiver addDirector(Long slotId, String email, String firstName, String lastName) {
        DiveSlot slot = DiveSlot.findById(slotId);
        SlotDiver d = new SlotDiver();
        d.slot       = slot;
        d.firstName  = firstName;
        d.lastName   = lastName;
        d.level      = "MF1";
        d.email      = email;
        d.isDirector = true;
        d.persist();
        return d;
    }

    @Transactional
    SlotDiver addDiver(Long slotId, String lastName, String level) {
        DiveSlot slot = DiveSlot.findById(slotId);
        SlotDiver d = new SlotDiver();
        d.slot       = slot;
        d.firstName  = "Test";
        d.lastName   = lastName;
        d.level      = level;
        d.isDirector = false;
        d.persist();
        return d;
    }

    @Transactional
    void cleanup(Long... slotIds) {
        for (Long slotId : slotIds) {
            if (slotId == null) continue;
            DiveSlot slot = DiveSlot.findById(slotId);
            if (slot == null) continue;
            SlotDiver.delete("slot", slot);
            User creator = slot.createdBy;
            slot.delete();
            // Ne pas supprimer le créateur si d'autres slots l'utilisent
            if (creator != null && DiveSlot.count("createdBy", creator) == 0) {
                creator.delete();
            }
        }
    }

    @Transactional
    void cleanupUser(String email) {
        User u = User.find("email", email).firstResult();
        if (u != null) u.delete();
    }

    // ── GET /api/stats (admin) ────────────────────────────────────────────────

    @Test
    @TestSecurity(user = "stats_admin@test.com", roles = {"ADMIN"})
    void getStats_asAdmin_shouldReturnGlobalStats() {
        User admin = createUser("stats_admin@test.com", "Admin", "STATS", UserRole.ADMIN);
        DiveSlot slot = createSlot(admin, LocalDate.of(2098, 3, 15));
        addDiver(slot.id, "DUPONT", "N2");
        addDiver(slot.id, "MARTIN", "N3");
        try {
            given()
                .queryParam("from", "2098-01-01")
                .queryParam("to", "2098-12-31")
                .when().get("/api/stats")
                .then()
                .statusCode(200)
                .body("totalSlots", equalTo(1))
                .body("totalDivers", equalTo(2));
        } finally {
            cleanup(slot.id);
        }
    }

    @Test
    @TestSecurity(user = "stats_dp_forbidden@test.com", roles = {"DIVE_DIRECTOR"})
    void getStats_asDp_shouldReturn403() {
        createUser("stats_dp_forbidden@test.com", "DP", "NOACCESS", UserRole.DIVE_DIRECTOR);
        try {
            given()
                .when().get("/api/stats")
                .then()
                .statusCode(403);
        } finally {
            cleanupUser("stats_dp_forbidden@test.com");
        }
    }

    // ── GET /api/stats/my ─────────────────────────────────────────────────────

    @Test
    @TestSecurity(user = "stats_my_dp@test.com", roles = {"DIVE_DIRECTOR"})
    void getMyStats_shouldIncludeCreatedSlots() {
        User dp = createUser("stats_my_dp@test.com", "DP", "MYSTATS", UserRole.DIVE_DIRECTOR);
        DiveSlot slot = createSlot(dp, LocalDate.of(2098, 5, 10));
        addDirector(slot.id, "stats_my_dp@test.com", "DP", "MYSTATS");
        addDiver(slot.id, "PLONGEUR1", "N2");
        try {
            given()
                .queryParam("from", "2098-01-01")
                .queryParam("to", "2098-12-31")
                .when().get("/api/stats/my")
                .then()
                .statusCode(200)
                .body("totalSlots", equalTo(1))
                .body("totalDivers", equalTo(2)); // directeur + plongeur
        } finally {
            cleanup(slot.id);
        }
    }

    @Test
    @TestSecurity(user = "stats_my_directed@test.com", roles = {"DIVE_DIRECTOR"})
    void getMyStats_shouldIncludeSlotsDirectedButNotCreated() {
        // DP qui sera directeur mais pas créateur
        User directorDp = createUser("stats_my_directed@test.com", "DP", "DIRECTED", UserRole.DIVE_DIRECTOR);
        // Autre DP qui crée le créneau
        User creatorDp = createUser("stats_other_creator@test.com", "Autre", "CREATOR", UserRole.DIVE_DIRECTOR);
        DiveSlot slot = createSlot(creatorDp, LocalDate.of(2098, 7, 20));
        // Le premier DP est inscrit comme directeur sur ce créneau
        addDirector(slot.id, "stats_my_directed@test.com", "DP", "DIRECTED");
        addDiver(slot.id, "PLONGEUR_A", "N3");
        addDiver(slot.id, "PLONGEUR_B", "N2");
        try {
            // Le DP dirigeant doit voir ce créneau dans ses stats
            given()
                .queryParam("from", "2098-01-01")
                .queryParam("to", "2098-12-31")
                .when().get("/api/stats/my")
                .then()
                .statusCode(200)
                .body("totalSlots", equalTo(1))
                .body("totalDivers", equalTo(3)); // directeur + 2 plongeurs
        } finally {
            cleanup(slot.id);
            cleanupUser("stats_my_directed@test.com");
        }
    }

    @Test
    @TestSecurity(user = "stats_my_both@test.com", roles = {"DIVE_DIRECTOR"})
    void getMyStats_shouldNotDuplicateWhenBothCreatorAndDirector() {
        // Le DP crée le créneau ET est inscrit comme directeur → pas de doublon
        User dp = createUser("stats_my_both@test.com", "DP", "BOTH", UserRole.DIVE_DIRECTOR);
        DiveSlot slot = createSlot(dp, LocalDate.of(2098, 9, 5));
        addDirector(slot.id, "stats_my_both@test.com", "DP", "BOTH");
        addDiver(slot.id, "PLONGEUR_X", "N1");
        try {
            given()
                .queryParam("from", "2098-01-01")
                .queryParam("to", "2098-12-31")
                .when().get("/api/stats/my")
                .then()
                .statusCode(200)
                .body("totalSlots", equalTo(1)) // pas 2 !
                .body("totalDivers", equalTo(2));
        } finally {
            cleanup(slot.id);
        }
    }

    @Test
    @TestSecurity(user = "stats_my_empty@test.com", roles = {"DIVE_DIRECTOR"})
    void getMyStats_shouldReturnZeros_whenNoDirections() {
        createUser("stats_my_empty@test.com", "DP", "EMPTY", UserRole.DIVE_DIRECTOR);
        try {
            given()
                .queryParam("from", "2098-01-01")
                .queryParam("to", "2098-12-31")
                .when().get("/api/stats/my")
                .then()
                .statusCode(200)
                .body("totalSlots", equalTo(0))
                .body("totalDivers", equalTo(0));
        } finally {
            cleanupUser("stats_my_empty@test.com");
        }
    }
}
