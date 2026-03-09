package org.santalina.diving.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Tests d'intégration de l'endpoint /api/slots.
 */
@QuarkusTest
class SlotResourceIT {

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

    /* ── Création de créneau (ADMIN) ── */

    @Test
    @TestSecurity(user = "admin@santalina.com", roles = {"ADMIN"})
    void createSlot_shouldReturn401_whenUserNotFoundInDB() {
        // L'utilisateur @TestSecurity n'existe pas en base H2 → NotAuthorizedException
        given()
                .contentType(ContentType.JSON)
                .body("""
                      {"slotDate":"2099-07-01","startTime":"09:00","endTime":"12:00",
                       "diverCount":5,"title":"Sortie test","slotType":"Club - Plongée"}
                      """)
                .when().post("/api/slots")
                .then()
                .statusCode(anyOf(is(401), is(403)));
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
}
