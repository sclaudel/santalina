package org.santalina.diving.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Tests d'intégration de l'endpoint /api/config.
 */
@QuarkusTest
class ConfigResourceIT {

    /* ── Accès public ── */

    @Test
    void getConfig_shouldReturn200_withoutAuthentication() {
        given()
                .when().get("/api/config")
                .then()
                .statusCode(200)
                .body("maxDivers", greaterThan(0))
                .body("siteName", notNullValue())
                .body("slotTypes", not(empty()));
    }

    /* ── Modification (ADMIN uniquement) ── */

    @Test
    void updateMaxDivers_shouldReturn401_withoutAuthentication() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"maxDivers\":30}")
                .when().put("/api/config/max-divers")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "admin@santalina.com", roles = {"ADMIN"})
    void updateMaxDivers_shouldReturn200_whenAdmin() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"maxDivers\":20}")
                .when().put("/api/config/max-divers")
                .then()
                .statusCode(200)
                .body("maxDivers", equalTo(20));
    }

    @Test
    @TestSecurity(user = "diver@test.com", roles = {"DIVER"})
    void updateMaxDivers_shouldReturn403_whenDiver() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"maxDivers\":30}")
                .when().put("/api/config/max-divers")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin@santalina.com", roles = {"ADMIN"})
    void updateSiteName_shouldReturn200_whenAdmin() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"siteName\":\"Carrière de Test\"}")
                .when().put("/api/config/site-name")
                .then()
                .statusCode(200)
                .body("siteName", equalTo("Carrière de Test"));
    }

    @Test
    @TestSecurity(user = "admin@santalina.com", roles = {"ADMIN"})
    void updatePublicAccess_shouldReturn200_whenAdmin() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"value\":true}")
                .when().put("/api/config/public-access")
                .then()
                .statusCode(200);
    }

    /* ── Notification settings ── */

    @Test
    void updateNotifSettings_shouldReturn401_withoutAuthentication() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                      {"notifRegistrationEnabled":true,"notifApprovedEnabled":true,
                       "notifCancelledEnabled":true,"notifMovedToWlEnabled":true,
                       "notifDpNewRegEnabled":true,"notifSafetyReminderEnabled":false,
                       "safetyReminderDelayDays":3,"safetyReminderEmailBody":"test"}
                      """)
                .when().put("/api/config/notification-settings")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "diver@test.com", roles = {"DIVER"})
    void updateNotifSettings_shouldReturn403_whenDiver() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                      {"notifRegistrationEnabled":false,"notifApprovedEnabled":false,
                       "notifCancelledEnabled":false,"notifMovedToWlEnabled":false,
                       "notifDpNewRegEnabled":false,"notifSafetyReminderEnabled":false,
                       "safetyReminderDelayDays":3,"safetyReminderEmailBody":"test"}
                      """)
                .when().put("/api/config/notification-settings")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin@santalina.com", roles = {"ADMIN"})
    void updateNotifSettings_shouldReturn200_whenAdmin() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                      {"notifRegistrationEnabled":false,"notifApprovedEnabled":true,
                       "notifCancelledEnabled":true,"notifMovedToWlEnabled":false,
                       "notifDpNewRegEnabled":true,"notifSafetyReminderEnabled":true,
                       "safetyReminderDelayDays":5,"safetyReminderEmailBody":"Rappel test {slotDate}"}
                      """)
                .when().put("/api/config/notification-settings")
                .then()
                .statusCode(200)
                .body("notifRegistrationEnabled", equalTo(false))
                .body("notifApprovedEnabled", equalTo(true))
                .body("notifMovedToWlEnabled", equalTo(false))
                .body("notifSafetyReminderEnabled", equalTo(true))
                .body("safetyReminderDelayDays", equalTo(5))
                .body("safetyReminderEmailBody", containsString("Rappel test"));
    }

    /* ── Durée maximale d'un créneau ── */

    @Test
    void updateSlotMaxHours_shouldReturn401_withoutAuthentication() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"slotMaxHours\":8}")
                .when().put("/api/config/slot-max-hours")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "admin@santalina.com", roles = {"ADMIN"})
    void updateSlotMaxHours_shouldReturn200_whenAdmin() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"slotMaxHours\":8}")
                .when().put("/api/config/slot-max-hours")
                .then()
                .statusCode(200)
                .body("slotMaxHours", equalTo(8));
    }

    @Test
    @TestSecurity(user = "diver@test.com", roles = {"DIVER"})
    void updateSlotMaxHours_shouldReturn403_whenDiver() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"slotMaxHours\":8}")
                .when().put("/api/config/slot-max-hours")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin@santalina.com", roles = {"ADMIN"})
    void updateSlotMaxHours_shouldReturn400_whenInvalidValue() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"slotMaxHours\":0}")
                .when().put("/api/config/slot-max-hours")
                .then()
                .statusCode(400);
    }
}
