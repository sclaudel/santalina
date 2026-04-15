package org.santalina.diving.integration;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.santalina.diving.service.CaptchaService;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests d'intégration de l'endpoint /api/auth.
 * Utilise la base H2 in-memory et un vrai contexte Quarkus.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthResourceIT {

    private static final String REGISTER_URL        = "/api/auth/register";
    private static final String LOGIN_URL            = "/api/auth/login";
    private static final String RESET_REQUEST_URL    = "/api/auth/password-reset/request";

    @InjectMock
    CaptchaService captchaService;

    @BeforeEach
    void setup() {
        // Bypass captcha validation in all integration tests
        when(captchaService.verify(any(), any())).thenReturn(true);
    }

    /* ── Inscription ── */

    @Test
    @Order(1)
    void register_shouldReturn200AndMessage_whenValidRequest() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                      {"email":"newuser@test.com","firstName":"Test","lastName":"User",
                       "phone":"+33600000001","consentGiven":true,
                       "captchaId":"test-id","captchaAnswer":"ABCDE"}
                      """)
                .when().post(REGISTER_URL)
                .then()
                .statusCode(200)
                .body("message", notNullValue());
    }

    @Test
    @Order(2)
    void register_shouldReturn400_whenEmailAlreadyExists() {
        // Tente de ré-inscrire le même email
        given()
                .contentType(ContentType.JSON)
                .body("""
                      {"email":"newuser@test.com","firstName":"Test","lastName":"User",
                       "phone":"+33600000001","consentGiven":true,
                       "captchaId":"test-id","captchaAnswer":"ABCDE"}
                      """)
                .when().post(REGISTER_URL)
                .then()
                .statusCode(400);
    }

    @Test
    @Order(3)
    void register_shouldReturn400_whenFirstNameIsBlank() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                      {"email":"other@test.com","firstName":"","lastName":"User",
                       "phone":"+33600000001","consentGiven":true,
                       "captchaId":"test-id","captchaAnswer":"ABCDE"}
                      """)
                .when().post(REGISTER_URL)
                .then()
                .statusCode(400);
    }

    @Test
    @Order(4)
    void register_shouldReturn400_whenEmailIsInvalid() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                      {"email":"not-an-email","firstName":"Test","lastName":"User",
                       "phone":"+33600000001","consentGiven":true,
                       "captchaId":"test-id","captchaAnswer":"ABCDE"}
                      """)
                .when().post(REGISTER_URL)
                .then()
                .statusCode(400);
    }

    @Test
    @Order(5)
    void register_withClub_shouldReturn200() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                      {"email":"club.user@test.com","firstName":"Club","lastName":"User",
                       "phone":"+33600000002","consentGiven":true,
                       "captchaId":"test-id","captchaAnswer":"ABCDE",
                       "club":"Club Santalina"}
                      """)
                .when().post(REGISTER_URL)
                .then()
                .statusCode(200)
                .body("message", notNullValue());
    }

    /* ── Connexion ── */

    @Test
    @Order(6)
    void login_shouldReturn200AndToken_whenValidCredentials() {
        // L'admin est créé automatiquement au démarrage par ensureAdminExists()
        // avec les credentials par défaut (config non surchargée en test)
        given()
                .contentType(ContentType.JSON)
                .body("""
                      {"email":"admin@santalina.com","password":"Admin1234"}
                      """)
                .when().post(LOGIN_URL)
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("email", equalTo("admin@santalina.com"));
    }

    @Test
    @Order(6)
    void login_shouldReturnClubAndLicenseInResponse() {
        // Vérifie que la réponse de connexion contient bien les clés club et licenseNumber
        // (elles peuvent être null pour l'admin, mais les champs doivent être présents)
        given()
                .contentType(ContentType.JSON)
                .body("""
                      {"email":"admin@santalina.com","password":"Admin1234"}
                      """)
                .when().post(LOGIN_URL)
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("$", hasKey("club"))
                .body("$", hasKey("licenseNumber"));
    }

    @Test
    @Order(7)
    void login_shouldReturn401_whenWrongPassword() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                      {"email":"admin@santalina.com","password":"WrongPassword"}
                      """)
                .when().post(LOGIN_URL)
                .then()
                .statusCode(401);
    }

    @Test
    @Order(8)
    void login_shouldReturn401_whenUserDoesNotExist() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                      {"email":"nobody@test.com","password":"Password1"}
                      """)
                .when().post(LOGIN_URL)
                .then()
                .statusCode(401);
    }

    /* ── Réinitialisation de mot de passe ── */

    @Test
    @Order(9)
    void passwordResetRequest_shouldReturn200_forAnyEmail() {
        // La réponse est identique qu'un compte existe ou non (pour éviter l'énumération)
        given()
                .contentType(ContentType.JSON)
                .body("""
                      {"email":"nobody@test.com"}
                      """)
                .when().post(RESET_REQUEST_URL)
                .then()
                .statusCode(200)
                .body("message", containsString("réinitialisation"));
    }

    /* ── Mode maintenance ── */

    @Test
    @Order(10)
    @io.quarkus.test.security.TestSecurity(user = "admin@santalina.com", roles = {"ADMIN"})
    void login_adminShouldSucceed_evenInMaintenanceMode() {
        // Activer le mode maintenance via l'API
        given()
                .contentType(ContentType.JSON)
                .body("{\"value\":true}")
                .when().put("/api/config/maintenance-mode")
                .then().statusCode(200);

        try {
            // L'admin doit pouvoir se connecter malgré le mode maintenance
            given()
                    .contentType(ContentType.JSON)
                    .body("""
                          {"email":"admin@santalina.com","password":"Admin1234"}
                          """)
                    .when().post(LOGIN_URL)
                    .then()
                    .statusCode(200)
                    .body("token", notNullValue());
        } finally {
            // Remettre l'état initial (désactiver le mode maintenance)
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"value\":false}")
                    .when().put("/api/config/maintenance-mode")
                    .then().statusCode(200);
        }
    }
}
