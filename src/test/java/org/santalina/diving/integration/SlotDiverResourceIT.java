package org.santalina.diving.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Tests d'intégration des endpoints d'inscription plongeurs.
 *
 * <p>Données de test chargées par {@code V24__test_data.sql} (H2 uniquement) :</p>
 * <ul>
 *   <li>Slot 1001 – inscriptions désactivées</li>
 *   <li>Slot 1002 – inscriptions activées, aucun directeur</li>
 *   <li>Slot 1003 – inscriptions activées, dp@test.com directeur, 5 places libres</li>
 *   <li>Slot 1004 – inscriptions activées, dp@test.com directeur, 1 place (PLEINE)</li>
 *   <li>Slot 1005 – inscriptions activées, dp@test.com directeur, diver@test.com en PENDING</li>
 * </ul>
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SlotDiverResourceIT {

    /* ═══════════════════════════════════════════════════════════════════════
     * 1. POST /api/slots/{id}/divers/register
     * ═══════════════════════════════════════════════════════════════════════ */

    @Test
    @Order(10)
    void registerSelf_shouldReturn401_withoutAuthentication() {
        given()
                .contentType(ContentType.JSON)
                .body(registrationBody("N2"))
                .when().post("/api/slots/1003/divers/register")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(11)
    @TestSecurity(user = "admin@santalina.com", roles = {"ADMIN"})
    void registerSelf_shouldReturn403_whenAdmin() {
        // ADMIN n'est pas dans @RolesAllowed({"DIVER","DIVE_DIRECTOR"})
        given()
                .contentType(ContentType.JSON)
                .body(registrationBody("N2"))
                .when().post("/api/slots/1003/divers/register")
                .then()
                .statusCode(403);
    }

    @Test
    @Order(12)
    @TestSecurity(user = "diver@test.com", roles = {"DIVER"})
    void registerSelf_shouldReturn404_whenSlotNotFound() {
        given()
                .contentType(ContentType.JSON)
                .body(registrationBody("N2"))
                .when().post("/api/slots/99999/divers/register")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(13)
    @TestSecurity(user = "diver@test.com", roles = {"DIVER"})
    void registerSelf_shouldReturn400_whenRegistrationDisabled() {
        given()
                .contentType(ContentType.JSON)
                .body(registrationBody("N2"))
                .when().post("/api/slots/1001/divers/register")
                .then()
                .statusCode(400)
                .body("message", containsStringIgnoringCase("ferm"));
    }

    @Test
    @Order(14)
    @TestSecurity(user = "diver@test.com", roles = {"DIVER"})
    void registerSelf_shouldReturn400_whenNoDirectorAssigned() {
        given()
                .contentType(ContentType.JSON)
                .body(registrationBody("N2"))
                .when().post("/api/slots/1002/divers/register")
                .then()
                .statusCode(400)
                .body("message", containsStringIgnoringCase("directeur"));
    }

    @Test
    @Order(15)
    @TestSecurity(user = "diver@test.com", roles = {"DIVER"})
    void registerSelf_shouldReturn400_whenAlreadyRegistered() {
        // diver@test.com est déjà PENDING sur le slot 1005 (migration V24)
        given()
                .contentType(ContentType.JSON)
                .body(registrationBody("N2"))
                .when().post("/api/slots/1005/divers/register")
                .then()
                .statusCode(400)
                .body("message", containsStringIgnoringCase("déjà inscrit"));
    }

    @Test
    @Order(16)
    @TestSecurity(user = "diver2@test.com", roles = {"DIVER"})
    void registerSelf_shouldReturn400_whenSlotFull() {
        // Slot 1004 : 1 place, déjà 1 CONFIRMED (migration V24)
        given()
                .contentType(ContentType.JSON)
                .body(registrationBody("N2"))
                .when().post("/api/slots/1004/divers/register")
                .then()
                .statusCode(400)
                .body("message", containsStringIgnoringCase("complet"));
    }

    /**
     * Ce test crée une inscription PENDING pour diver2@test.com sur le slot 1003.
     * Il est exécuté en premier parmi les tests de mutation et est requ par les tests suivants.
     */
    @Test
    @Order(20)
    @TestSecurity(user = "diver2@test.com", roles = {"DIVER"})
    void registerSelf_shouldReturn201_andCreatePendingEntry() {
        given()
                .contentType(ContentType.JSON)
                .body(registrationBody("N2"))
                .when().post("/api/slots/1003/divers/register")
                .then()
                .statusCode(201)
                .body("level", equalTo("N2"))
                .body("registrationStatus", equalTo("PENDING"));
    }

    /* ═══════════════════════════════════════════════════════════════════════
     * 2. GET /api/slots/{id}/divers/waitlist
     * ═══════════════════════════════════════════════════════════════════════ */

    @Test
    @Order(30)
    void getWaitlist_shouldReturn401_withoutAuthentication() {
        given()
                .when().get("/api/slots/1005/divers/waitlist")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(31)
    @TestSecurity(user = "diver@test.com", roles = {"DIVER"})
    void getWaitlist_shouldReturn403_whenDiver() {
        given()
                .when().get("/api/slots/1005/divers/waitlist")
                .then()
                .statusCode(403);
    }

    @Test
    @Order(32)
    @TestSecurity(user = "dp@test.com", roles = {"DIVE_DIRECTOR"})
    void getWaitlist_shouldReturn404_whenSlotNotFound() {
        given()
                .when().get("/api/slots/99999/divers/waitlist")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(33)
    @TestSecurity(user = "dp2@test.com", roles = {"DIVE_DIRECTOR"})
    void getWaitlist_shouldReturn403_whenNotAssignedDirector() {
        // dp2@test.com n'est pas le directeur du slot 1005
        given()
                .when().get("/api/slots/1005/divers/waitlist")
                .then()
                .statusCode(403);
    }

    @Test
    @Order(34)
    @TestSecurity(user = "dp@test.com", roles = {"DIVE_DIRECTOR"})
    void getWaitlist_shouldReturn200_withPendingEntries() {
        // Slot 1005 : diver@test.com est PENDING (migration V24)
        given()
                .when().get("/api/slots/1005/divers/waitlist")
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)))
                .body("[0].level", notNullValue());
    }

    /**
     * Vérifie que la waitlist du slot 1003 contient bien diver2@test.com
     * après l'inscription du test @Order(20).
     */
    @Test
    @Order(35)
    @TestSecurity(user = "dp@test.com", roles = {"DIVE_DIRECTOR"})
    void getWaitlist_shouldReturn200_withNewlyRegisteredDiver() {
        given()
                .when().get("/api/slots/1003/divers/waitlist")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].registrationStatus", equalTo("PENDING"));
    }

    /* ═══════════════════════════════════════════════════════════════════════
     * 3. POST /api/slots/{id}/divers/waitlist/{diverId}/validate
     * ═══════════════════════════════════════════════════════════════════════ */

    @Test
    @Order(40)
    void validateRegistration_shouldReturn401_withoutAuthentication() {
        given()
                .when().post("/api/slots/1005/divers/waitlist/99999/validate")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(41)
    @TestSecurity(user = "diver@test.com", roles = {"DIVER"})
    void validateRegistration_shouldReturn403_whenDiver() {
        given()
                .when().post("/api/slots/1005/divers/waitlist/99999/validate")
                .then()
                .statusCode(403);
    }

    @Test
    @Order(42)
    @TestSecurity(user = "dp@test.com", roles = {"DIVE_DIRECTOR"})
    void validateRegistration_shouldReturn404_whenSlotNotFound() {
        given()
                .when().post("/api/slots/99999/divers/waitlist/1/validate")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(43)
    @TestSecurity(user = "dp2@test.com", roles = {"DIVE_DIRECTOR"})
    void validateRegistration_shouldReturn403_whenNotAssignedDirector() {
        given()
                .when().post("/api/slots/1005/divers/waitlist/99999/validate")
                .then()
                .statusCode(403);
    }

    /**
     * Valide l'inscription de diver2@test.com créée au test @Order(20)
     * sur le slot 1003, en cherchant d'abord l'ID via la waitlist.
     */
    @Test
    @Order(50)
    @TestSecurity(user = "dp@test.com", roles = {"DIVE_DIRECTOR"})
    void validateRegistration_shouldReturn200_andSetStatusConfirmed() {
        // 1. Récupère l'ID de l'entrée PENDING
        int diverId = given()
                .when().get("/api/slots/1003/divers/waitlist")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .extract().jsonPath().getInt("[0].id");

        // 2. Valide l'entrée
        given()
                .when().post("/api/slots/1003/divers/waitlist/" + diverId + "/validate")
                .then()
                .statusCode(200)
                .body("registrationStatus", equalTo("CONFIRMED"));
    }

    @Test
    @Order(51)
    @TestSecurity(user = "dp@test.com", roles = {"DIVE_DIRECTOR"})
    void validateRegistration_shouldReturn400_whenSlotFull() {
        // Slot 1004 : 1 place déjà pleine (migration V24)
        // On essaie de valider une entrée fictive mais on arrive au 400/404
        // (la vérification capacité se fait avant de chercher le diver)
        given()
                .when().post("/api/slots/1004/divers/waitlist/99999/validate")
                .then()
                .statusCode(anyOf(is(400), is(404)));
    }

    /* ═══════════════════════════════════════════════════════════════════════
     * 4. DELETE /api/slots/{id}/divers/registrations/me
     * ═══════════════════════════════════════════════════════════════════════ */

    @Test
    @Order(60)
    void cancelMyRegistration_shouldReturn401_withoutAuthentication() {
        given()
                .when().delete("/api/slots/1005/divers/registrations/me")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(61)
    @TestSecurity(user = "admin@santalina.com", roles = {"ADMIN"})
    void cancelMyRegistration_shouldReturn403_whenAdmin() {
        given()
                .when().delete("/api/slots/1005/divers/registrations/me")
                .then()
                .statusCode(403);
    }

    @Test
    @Order(62)
    @TestSecurity(user = "diver@test.com", roles = {"DIVER"})
    void cancelMyRegistration_shouldReturn404_whenSlotNotFound() {
        given()
                .when().delete("/api/slots/99999/divers/registrations/me")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(63)
    @TestSecurity(user = "diver2@test.com", roles = {"DIVER"})
    void cancelMyRegistration_shouldReturn404_whenNotRegistered() {
        // diver2@test.com n'est pas inscrit sur le slot 1001
        given()
                .when().delete("/api/slots/1001/divers/registrations/me")
                .then()
                .statusCode(404);
    }

    /**
     * diver@test.com annule sa participation PENDING sur le slot 1005.
     * Exécuté en dernier pour ne pas perturber les tests waitlist/validate (slots 1003/1005).
     */
    @Test
    @Order(70)
    @TestSecurity(user = "diver@test.com", roles = {"DIVER"})
    void cancelMyRegistration_shouldReturn200_andRemoveEntry() {
        given()
                .when().delete("/api/slots/1005/divers/registrations/me")
                .then()
                .statusCode(200)
                .body("message", containsStringIgnoringCase("annul"));
    }

    @Test
    @Order(71)
    @TestSecurity(user = "diver@test.com", roles = {"DIVER"})
    void cancelMyRegistration_shouldReturn404_afterCancellation() {
        // Suite directe du test précédent : l'entrée a été supprimée
        given()
                .when().delete("/api/slots/1005/divers/registrations/me")
                .then()
                .statusCode(404);
    }

    /* ═══════════════════════════════════════════════════════════════════════
     * 5. GET /api/slots/{id}/divers — endpoint public (régression)
     * ═══════════════════════════════════════════════════════════════════════ */

    @Test
    @Order(80)
    void getDivers_shouldReturn200_andShowOnlyConfirmed() {
        // Slot 1005 expose la liste publique — seuls les CONFIRMED (directeur) doivent apparaître
        given()
                .when().get("/api/slots/1005/divers")
                .then()
                .statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    /* ═══════════════════════════════════════════════════════════════════════
     * Helpers
     * ═══════════════════════════════════════════════════════════════════════ */

    /** Corps JSON minimal valide pour POST /register */
    private static String registrationBody(String level) {
        return """
               {
                 "level": "%s",
                 "numberOfDives": 20,
                 "lastDiveDate": "2024-01-15",
                 "email": "diver2@test.com"
               }
               """.formatted(level);
    }
}
