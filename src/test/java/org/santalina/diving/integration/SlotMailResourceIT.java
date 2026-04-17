package org.santalina.diving.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Tests d'intégration des endpoints /api/slots/{slotId}/mail.
 */
@QuarkusTest
class SlotMailResourceIT {

    private static final String MAIL_PATH = "/api/slots/{slotId}/mail/organization";
    private static final long   UNKNOWN_SLOT = 999999L;

    /* ── Accès protégé (non authentifié) ── */

    @Test
    void sendOrganizationMail_shouldReturn401_withoutAuthentication() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"subject\":\"Test\",\"htmlBody\":\"<p>Hello</p>\"}")
                .pathParam("slotId", 1)
                .when().post(MAIL_PATH)
                .then()
                .statusCode(401);
    }

    /* ── Contrôle des rôles ── */

    @Test
    @TestSecurity(user = "diver@test.com", roles = {"DIVER"})
    void sendOrganizationMail_shouldReturn403_whenDiver() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"subject\":\"Test\",\"htmlBody\":\"<p>Hello</p>\"}")
                .pathParam("slotId", 1)
                .when().post(MAIL_PATH)
                .then()
                .statusCode(403);
    }

    /* ── Validation des entrées ── */

    @Test
    @TestSecurity(user = "admin@santalina.com", roles = {"ADMIN"})
    void sendOrganizationMail_shouldReturn404_whenSlotNotFound() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"subject\":\"Sortie\",\"htmlBody\":\"<p>Bonjour</p>\"}")
                .pathParam("slotId", UNKNOWN_SLOT)
                .when().post(MAIL_PATH)
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "admin@santalina.com", roles = {"ADMIN"})
    void sendOrganizationMail_shouldReturn400_whenSubjectMissing() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"subject\":\"\",\"htmlBody\":\"<p>Bonjour</p>\"}")
                .pathParam("slotId", 1)
                .when().post(MAIL_PATH)
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "admin@santalina.com", roles = {"ADMIN"})
    void sendOrganizationMail_shouldReturn400_whenBodyMissing() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"subject\":\"Sortie\",\"htmlBody\":\"\"}")
                .pathParam("slotId", 1)
                .when().post(MAIL_PATH)
                .then()
                .statusCode(400);
    }

    /* ── Endpoint de template DP ── */

    @Test
    void updateDpEmailTemplate_shouldReturn401_withoutAuthentication() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"template\":\"<p>Bonjour</p>\"}")
                .when().put("/api/users/me/dp-email-template")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "diver@test.com", roles = {"DIVER"})
    void updateDpEmailTemplate_shouldReturn403_whenDiver() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"template\":\"<p>Bonjour</p>\"}")
                .when().put("/api/users/me/dp-email-template")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin@santalina.com", roles = {"ADMIN"})
    void updateDpEmailTemplate_shouldReturn200_whenAdmin() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"template\":\"<p>Mon modèle</p>\"}")
                .when().put("/api/users/me/dp-email-template")
                .then()
                .statusCode(200)
                .body("dpOrganizerEmailTemplate", equalTo("<p>Mon modèle</p>"));
    }
}
