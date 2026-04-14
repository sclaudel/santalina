package org.santalina.diving.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.domain.User;
import org.santalina.diving.domain.UserRole;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Tests d'intégration de l'endpoint /api/slots.
 */
@QuarkusTest
class SlotResourceIT {

    // ── fixtures ──────────────────────────────────────────────────────────────

    @Transactional
    long createDiveDirector(String email, String lastName) {
        User dp = new User();
        dp.email        = email;
        dp.firstName    = "DP";
        dp.lastName     = lastName;
        dp.passwordHash = "x";
        dp.activated    = true;
        dp.role         = UserRole.DIVE_DIRECTOR;
        dp.roles        = java.util.Set.of(UserRole.DIVE_DIRECTOR);
        dp.persist();
        return dp.id;
    }

    @Transactional
    long createAdminUser(String email) {
        User admin = new User();
        admin.email        = email;
        admin.firstName    = "Admin";
        admin.lastName     = "TEST";
        admin.passwordHash = "x";
        admin.activated    = true;
        admin.role         = UserRole.ADMIN;
        admin.roles        = java.util.Set.of(UserRole.ADMIN);
        admin.persist();
        return admin.id;
    }

    @Transactional
    long createDiver(String email) {
        User diver = new User();
        diver.email        = email;
        diver.firstName    = "Diver";
        diver.lastName     = "TEST";
        diver.passwordHash = "x";
        diver.activated    = true;
        diver.role         = UserRole.DIVER;
        diver.roles        = java.util.Set.of(UserRole.DIVER);
        diver.persist();
        return diver.id;
    }

    @Transactional
    void cleanupUser(String email) {
        User u = User.findByEmail(email);
        if (u != null) {
            DiveSlot.update("createdBy = null WHERE createdBy = ?1", u);
            u.delete();
        }
    }

    /* ── Accès public ── */

    @Test
    void getSlotsByDate_shouldReturn200_withoutAuthentication() {
        given()
                .queryParam("date", "2099-06-15")
                .when().get("/api/slots")
                .then()
                .statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void getSlotsByWeek_shouldReturn200_withoutAuthentication() {
        given()
                .queryParam("from", "2099-06-10")
                .when().get("/api/slots/week")
                .then()
                .statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void getSlotsByMonth_shouldReturn200_withoutAuthentication() {
        given()
                .queryParam("year", "2099")
                .queryParam("month", "6")
                .when().get("/api/slots/month")
                .then()
                .statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void getSlotById_shouldReturn404_whenNotFound() {
        given()
                .when().get("/api/slots/99999")
                .then()
                .statusCode(404);
    }

    /* ── Création de créneau ── */

    @Test
    @TestSecurity(user = "diver@test.com", roles = {"DIVER"})
    void createSlot_shouldReturn403_whenRoleIsDiver() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                      {"slotDate":"2099-07-01","startTime":"09:00","endTime":"12:00",
                       "diverCount":5,"title":"Sortie test","slotType":"Club - Plongée"}
                      """)
                .when().post("/api/slots")
                .then()
                .statusCode(403);
    }

    @Test
    void createSlot_shouldReturn401_withoutAuthentication() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                      {"slotDate":"2099-07-01","startTime":"09:00","endTime":"12:00",
                       "diverCount":5,"title":"Sortie test","slotType":"Club - Plongée"}
                      """)
                .when().post("/api/slots")
                .then()
                .statusCode(401);
    }

    @Test
    void deleteSlot_shouldReturn401_withoutAuthentication() {
        given()
                .when().delete("/api/slots/1")
                .then()
                .statusCode(401);
    }

    /* ── Modification d'un créneau ── */

    @Test
    void updateSlotInfo_shouldReturn401_withoutAuthentication() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"slotDate\":\"2099-08-01\",\"startTime\":\"10:00\",\"endTime\":\"13:00\"}")
                .when().patch("/api/slots/1/info")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "diver@test.com", roles = {"DIVER"})
    void updateSlotInfo_shouldReturn403_whenRoleIsDiver() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"slotDate\":\"2099-08-01\",\"startTime\":\"10:00\",\"endTime\":\"13:00\"}")
                .when().patch("/api/slots/1/info")
                .then()
                .statusCode(403);
    }

    /* ── Création pour le compte d'un autre DP (createdByUserId) ── */

    @Test
    @TestSecurity(user = "admin_proxy1@test.com", roles = {"ADMIN"})
    void createSlot_asAdmin_withValidDpId_shouldReturn201AndAttributeSlotToDp() {
        createAdminUser("admin_proxy1@test.com");
        long dpId = createDiveDirector("dp_proxy_t1@test.com", "TARGET1");
        try {
            given()
                    .contentType(ContentType.JSON)
                    .body("""
                          {"slotDate":"2099-08-01","startTime":"09:00","endTime":"12:00",
                           "diverCount":6,"createdByUserId":%d}
                          """.formatted(dpId))
                    .when().post("/api/slots")
                    .then()
                    .statusCode(201)
                    .body("slots[0].createdByName", equalTo("DP TARGET1"));
        } finally {
            cleanupUser("dp_proxy_t1@test.com");
            cleanupUser("admin_proxy1@test.com");
        }
    }

    @Test
    @TestSecurity(user = "dp_noproxy@test.com", roles = {"DIVE_DIRECTOR"})
    void createSlot_asDiveDirector_withCreatedByUserId_shouldIgnoreFieldAndUseOwnAccount() {
        // Un DIVE_DIRECTOR ne peut pas créer pour le compte d'autrui — le champ est ignoré
        createDiveDirector("dp_noproxy@test.com", "NOPROXY");
        long dpOtherId = createDiveDirector("dp_proxy_other@test.com", "OTHER");
        try {
            given()
                    .contentType(ContentType.JSON)
                    .body("""
                          {"slotDate":"2099-08-02","startTime":"09:00","endTime":"12:00",
                           "diverCount":4,"createdByUserId":%d}
                          """.formatted(dpOtherId))
                    .when().post("/api/slots")
                    .then()
                    .statusCode(201)
                    // Le créneau doit être créé pour dp_noproxy (lui-même), pas dp_proxy_other
                    .body("slots[0].createdByName", equalTo("DP NOPROXY"));
        } finally {
            cleanupUser("dp_proxy_other@test.com");
            cleanupUser("dp_noproxy@test.com");
        }
    }

    @Test
    @TestSecurity(user = "admin_proxy2@test.com", roles = {"ADMIN"})
    void createSlot_asAdmin_withNonExistentDpId_shouldReturn404() {
        createAdminUser("admin_proxy2@test.com");
        try {
            given()
                    .contentType(ContentType.JSON)
                    .body("""
                          {"slotDate":"2099-09-01","startTime":"09:00","endTime":"12:00",
                           "diverCount":5,"createdByUserId":999999}
                          """)
                    .when().post("/api/slots")
                    .then()
                    .statusCode(404);
        } finally {
            cleanupUser("admin_proxy2@test.com");
        }
    }

    @Test
    @TestSecurity(user = "admin_proxy3@test.com", roles = {"ADMIN"})
    void createSlot_asAdmin_withNonDpUserId_shouldReturn400() {
        createAdminUser("admin_proxy3@test.com");
        long diverId = createDiver("diver_proxy_t3@test.com");
        try {
            given()
                    .contentType(ContentType.JSON)
                    .body("""
                          {"slotDate":"2099-09-02","startTime":"09:00","endTime":"12:00",
                           "diverCount":5,"createdByUserId":%d}
                          """.formatted(diverId))
                    .when().post("/api/slots")
                    .then()
                    .statusCode(400);
        } finally {
            cleanupUser("diver_proxy_t3@test.com");
            cleanupUser("admin_proxy3@test.com");
        }
    }
}
