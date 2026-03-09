package org.santalina.diving;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
public class SlotResourceTest {

    @Test
    void testGetSlotsPublic() {
        RestAssured.given()
                .when().get("/api/slots")
                .then()
                .statusCode(200);
    }

    @Test
    void testGetConfigPublic() {
        RestAssured.given()
                .when().get("/api/config")
                .then()
                .statusCode(200)
                .body("maxDivers", notNullValue());
    }

    @Test
    void testLoginInvalid() {
        RestAssured.given()
                .header("Content-Type", "application/json")
                .body("{\"email\":\"unknown@test.com\",\"password\":\"wrong\"}")
                .when().post("/api/auth/login")
                .then()
                .statusCode(401);
    }
}
