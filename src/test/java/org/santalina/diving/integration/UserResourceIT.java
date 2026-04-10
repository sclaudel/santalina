package org.santalina.diving.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Tests d'intégration de l'endpoint /api/users.
 */
@QuarkusTest
class UserResourceIT {

    /* ── Accès protégé (non authentifié) ── */

    @Test
    void getProfile_shouldReturn401_withoutAuthentication() {
        given()
                .when().get("/api/users/me")
                .then()
                .statusCode(401);
    }

    @Test
    void getAllUsers_shouldReturn401_withoutAuthentication() {
        given()
                .when().get("/api/users")
                .then()
                .statusCode(401);
    }

    @Test
    void searchUsers_shouldReturn401_withoutAuthentication() {
        given()
                .queryParam("q", "test")
                .when().get("/api/users/search")
                .then()
                .statusCode(401);
    }

    /* ── Contrôle des rôles ── */

    @Test
    @TestSecurity(user = "diver@test.com", roles = {"DIVER"})
    void getAllUsers_shouldReturn403_whenDiver() {
        given()
                .when().get("/api/users")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "diver@test.com", roles = {"DIVER"})
    void searchUsers_shouldReturn403_whenDiver() {
        given()
                .queryParam("q", "test")
                .when().get("/api/users/search")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin@santalina.com", roles = {"ADMIN"})
    void getAllUsers_shouldReturn200_whenAdmin() {
        given()
                .when().get("/api/users")
                .then()
                .statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    @TestSecurity(user = "admin@santalina.com", roles = {"ADMIN"})
    void searchUsers_shouldReturn200_whenAdmin() {
        given()
                .queryParam("q", "admin")
                .when().get("/api/users/search")
                .then()
                .statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    @TestSecurity(user = "admin@santalina.com", roles = {"ADMIN"})
    void createUser_shouldReturn400_whenEmailIsInvalid() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                      {"email":"not-an-email","password":"Password1",
                       "name":"Test","phone":"+33600000001","role":"DIVER"}
                      """)
                .when().post("/api/users")
                .then()
                .statusCode(400);
    }

    /* ── Notification prefs ── */

    @Test
    void updateNotifPrefs_shouldReturn401_withoutAuthentication() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                      {"notifOnRegistration":true,"notifOnApproved":true,
                       "notifOnCancelled":true,"notifOnMovedToWaitlist":true,"notifOnDpRegistration":true}
                      """)
                .when().put("/api/users/me/notifications")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "diver_notif@test.com", roles = {"DIVER"})
    void updateNotifPrefs_shouldReturn200_whenAuthenticated() {
        // Créer l'utilisateur dans la base
        createTestUser("diver_notif@test.com");
        try {
            given()
                    .contentType(ContentType.JSON)
                    .body("""
                          {"notifOnRegistration":false,"notifOnApproved":true,
                           "notifOnCancelled":false,"notifOnMovedToWaitlist":true,"notifOnDpRegistration":true}
                          """)
                    .when().put("/api/users/me/notifications")
                    .then()
                    .statusCode(200)
                    .body("notifOnRegistration", equalTo(false))
                    .body("notifOnApproved", equalTo(true))
                    .body("notifOnCancelled", equalTo(false));
        } finally {
            deleteTestUser("diver_notif@test.com");
        }
    }

    @jakarta.transaction.Transactional
    void createTestUser(String email) {
        org.santalina.diving.domain.User u = new org.santalina.diving.domain.User();
        u.email = email;
        u.firstName = "Test";
        u.lastName = "Notif";
        u.passwordHash = "x";
        u.activated = true;
        u.role = org.santalina.diving.domain.UserRole.DIVER;
        u.roles = java.util.Set.of(org.santalina.diving.domain.UserRole.DIVER);
        u.persist();
    }

    @jakarta.transaction.Transactional
    void deleteTestUser(String email) {
        org.santalina.diving.domain.User u = org.santalina.diving.domain.User.findByEmail(email);
        if (u != null) u.delete();
    }
}
