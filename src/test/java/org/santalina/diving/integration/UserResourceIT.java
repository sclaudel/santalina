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

    @Test
    @TestSecurity(user = "admin@santalina.com", roles = {"ADMIN"})
    void createUser_shouldReturn201_whenPhoneAndLicenseAreEmpty() {
        String testEmail = "no_phone_license@test.com";
        deleteTestUser(testEmail);
        try {
            given()
                    .contentType(ContentType.JSON)
                    .body("""
                          {"email":"%s","password":"Password1@",
                           "firstName":"Sans","lastName":"Telephone",
                           "phone":"","licenseNumber":"","club":"",
                           "roles":["DIVER"]}
                          """.formatted(testEmail))
                    .when().post("/api/users")
                    .then()
                    .statusCode(201);
        } finally {
            deleteTestUser(testEmail);
        }
    }

    @Test
    @TestSecurity(user = "admin@santalina.com", roles = {"ADMIN"})
    void createUser_shouldReturn400_whenPhoneFormatIsInvalid() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                      {"email":"bad_phone@test.com","password":"Password1",
                       "firstName":"Bad","lastName":"Phone",
                       "phone":"12345","licenseNumber":"",
                       "roles":["DIVER"]}
                      """)
                .when().post("/api/users")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "admin@santalina.com", roles = {"ADMIN"})
    void createUser_shouldReturn400_whenLicenseFormatIsInvalid() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                      {"email":"bad_license@test.com","password":"Password1",
                       "firstName":"Bad","lastName":"License",
                       "phone":"","licenseNumber":"INVALID-FORMAT",
                       "roles":["DIVER"]}
                      """)
                .when().post("/api/users")
                .then()
                .statusCode(400);
    }


    @Test
    void updateNotifPrefs_shouldReturn401_withoutAuthentication() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                      {"notifOnRegistration":true,"notifOnApproved":true,
                       "notifOnCancelled":true,"notifOnMovedToWaitlist":true,"notifOnDpRegistration":true,
                       "notifOnCreatorRegistration":false,"notifOnSafetyReminder":true}
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
                           "notifOnCancelled":false,"notifOnMovedToWaitlist":true,"notifOnDpRegistration":true,
                           "notifOnCreatorRegistration":false,"notifOnSafetyReminder":true}
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

    @Test
    @TestSecurity(user = "diver_club@test.com", roles = {"DIVER"})
    void updateProfile_withClub_shouldPersistClub() {
        createTestUser("diver_club@test.com");
        try {
            given()
                    .contentType(ContentType.JSON)
                    .body("""
                          {"firstName":"Club","lastName":"Tester",
                           "phone":null,"licenseNumber":null,
                           "club":"Club Santalina"}
                          """)
                    .when().put("/api/users/me")
                    .then()
                    .statusCode(200)
                    .body("club", equalTo("Club Santalina"));
        } finally {
            deleteTestUser("diver_club@test.com");
        }
    }

    @Test
    @TestSecurity(user = "admin@santalina.com", roles = {"ADMIN"})
    void createUser_withClub_shouldReturnClubInResponse() {
        String testEmail = "create_club_test@test.com";
        // Ensure clean state
        deleteTestUser(testEmail);
        try {
            given()
                    .contentType(ContentType.JSON)
                    .body("""
                          {"email":"%s","password":"Password1@",
                           "firstName":"Club","lastName":"Admin",
                           "phone":"+33600000099","licenseNumber":null,
                           "club":"Club Santalina",
                           "roles":["DIVER"]}
                          """.formatted(testEmail))
                    .when().post("/api/users")
                    .then()
                    .statusCode(201)
                    .body("club", equalTo("Club Santalina"));
        } finally {
            deleteTestUser(testEmail);
        }
    }

    /* ── Export / Import CSV ── */

    @Test
    void exportUsersCsv_shouldReturn401_withoutAuthentication() {
        given()
                .when().get("/api/users/export/csv")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "diver@test.com", roles = {"DIVER"})
    void exportUsersCsv_shouldReturn403_whenDiver() {
        given()
                .when().get("/api/users/export/csv")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin@santalina.com", roles = {"ADMIN"})
    void exportUsersCsv_shouldReturn200_withCsvHeader_whenAdmin() {
        given()
                .when().get("/api/users/export/csv")
                .then()
                .statusCode(200)
                .contentType(containsString("text/csv"))
                .body(startsWith("club;nom;prenom;email;telephone;licence"));
    }

    @Test
    void importUsersCsv_shouldReturn401_withoutAuthentication() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                      {"csvContent":"club;nom;prenom;email;telephone;licence","password":"Pass123"}
                      """)
                .when().post("/api/users/import/csv")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "admin@santalina.com", roles = {"ADMIN"})
    void importUsersCsv_shouldSkipExistingUser_whenAdmin() {
        String testEmail = "csv_existing@test.com";
        createTestUser(testEmail);
        try {
            String csv = "club;nom;prenom;email;telephone;licence\n"
                    + "Club Test;DUPONT;Jean;" + testEmail + ";0600000001;LIC001\n";
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"csvContent\":" + jsonString(csv) + ",\"password\":\"Pass123\"}")
                    .when().post("/api/users/import/csv")
                    .then()
                    .statusCode(200)
                    .body("imported", equalTo(0))
                    .body("skipped", equalTo(1))
                    .body("errors", equalTo(0));
        } finally {
            deleteTestUser(testEmail);
        }
    }

    @Test
    @TestSecurity(user = "admin@santalina.com", roles = {"ADMIN"})
    void importUsersCsv_shouldImportNewUser_whenAdmin() {
        String testEmail = "csv_new_import@test.com";
        deleteTestUser(testEmail);
        try {
            String csv = "club;nom;prenom;email;telephone;licence\n"
                    + "Club Import;MARTIN;Sophie;" + testEmail + ";0600000002;LIC002\n";
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"csvContent\":" + jsonString(csv) + ",\"password\":\"Pass1234\"}")
                    .when().post("/api/users/import/csv")
                    .then()
                    .statusCode(200)
                    .body("imported", equalTo(1))
                    .body("skipped", equalTo(0))
                    .body("errors", equalTo(0));
        } finally {
            deleteTestUser(testEmail);
        }
    }

    /** Encode une chaîne Java en littéral JSON (guillemets et retours à la ligne échappés). */
    private static String jsonString(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }

    /* ── GET /api/users/dive-directors ── */

    @Test
    void getDiveDirectors_shouldReturn401_withoutAuthentication() {
        given()
                .when().get("/api/users/dive-directors")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "diver@test.com", roles = {"DIVER"})
    void getDiveDirectors_shouldReturn403_whenDiver() {
        given()
                .when().get("/api/users/dive-directors")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "dp@test.com", roles = {"DIVE_DIRECTOR"})
    void getDiveDirectors_shouldReturn403_whenDiveDirector() {
        given()
                .when().get("/api/users/dive-directors")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin@santalina.com", roles = {"ADMIN"})
    void getDiveDirectors_shouldReturn200AndListDps_whenAdmin() {
        String dpEmail = "dp_list_test@test.com";
        createTestDiveDirector(dpEmail);
        try {
            given()
                    .when().get("/api/users/dive-directors")
                    .then()
                    .statusCode(200)
                    .body("$", instanceOf(java.util.List.class))
                    .body("email", hasItem(dpEmail));
        } finally {
            deleteTestUser(dpEmail);
        }
    }

    @jakarta.transaction.Transactional
    void createTestDiveDirector(String email) {
        org.santalina.diving.domain.User u = new org.santalina.diving.domain.User();
        u.email        = email;
        u.firstName    = "DP";
        u.lastName     = "List Test";
        u.passwordHash = "x";
        u.activated    = true;
        u.role         = org.santalina.diving.domain.UserRole.DIVE_DIRECTOR;
        u.roles        = java.util.Set.of(org.santalina.diving.domain.UserRole.DIVE_DIRECTOR);
        u.persist();
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
