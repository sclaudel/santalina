package org.santalina.diving.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.santalina.diving.domain.FreeDiveSession;
import org.santalina.diving.domain.FreePalanquee;
import org.santalina.diving.domain.FreePalanqueeMember;
import org.santalina.diving.domain.FreeSessionDive;
import org.santalina.diving.domain.FreeSessionDiver;
import org.santalina.diving.domain.User;
import org.santalina.diving.domain.UserRole;

import java.time.LocalDate;
import java.time.LocalTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Tests d'intégration pour FreeSessionResource.
 *
 * Couvre :
 *   - CRUD sessions (create, list, update, delete)
 *   - Limite max 15 sessions par DP
 *   - CRUD plongeurs (add, update, remove, doublon de nom)
 *   - CRUD plongées (create, patch, delete)
 *   - CRUD palanquées (create, rename+depth+duration, delete)
 *   - Assignation / désassignation de plongeur dans une palanquée
 *   - Réordonnancement des membres d'une palanquée
 *   - Aptitudes spécifiques (PATCH /members/{did}/aptitudes)
 *   - Accès refusé pour un propriétaire différent
 */
@QuarkusTest
class FreeSessionResourceIT {

    private static final String BASE = "/api/free-sessions";

    // ── fixtures ──────────────────────────────────────────────────────────────

    @Transactional
    User createDp(String email) {
        User u = new User();
        u.email        = email;
        u.firstName    = "DP";
        u.lastName     = "TEST";
        u.passwordHash = "x";
        u.activated    = true;
        u.role         = UserRole.DIVE_DIRECTOR;
        u.roles        = java.util.Set.of(UserRole.DIVE_DIRECTOR);
        u.persist();
        return u;
    }

    @Transactional
    FreeDiveSession createSession(String ownerEmail) {
        User owner = User.find("email", ownerEmail).firstResult();
        if (owner == null) owner = createDp(ownerEmail);
        FreeDiveSession s = new FreeDiveSession();
        s.owner     = owner;
        s.label     = "Org Test";
        s.diveDate  = LocalDate.of(2099, 6, 15);
        s.startTime = LocalTime.of(9, 0);
        s.persist();
        return s;
    }

    @Transactional
    FreeSessionDiver addDiver(Long sessionId, String lastName, String level) {
        FreeDiveSession session = FreeDiveSession.findById(sessionId);
        FreeSessionDiver d = new FreeSessionDiver();
        d.session    = session;
        d.firstName  = "Test";
        d.lastName   = lastName;
        d.level      = level;
        d.isDirector = false;
        d.persist();
        return d;
    }

    @Transactional
    FreeSessionDive addDive(Long sessionId, String label) {
        FreeDiveSession session = FreeDiveSession.findById(sessionId);
        FreeSessionDive dive = new FreeSessionDive();
        dive.session   = session;
        dive.diveIndex = (int) FreeSessionDive.count("session.id", sessionId) + 1;
        dive.label     = label;
        dive.startTime = LocalTime.of(9, 0);
        dive.persist();
        return dive;
    }

    @Transactional
    FreePalanquee addPalanquee(Long sessionId, String name) {
        FreeDiveSession session = FreeDiveSession.findById(sessionId);
        long count = FreePalanquee.count("session.id", sessionId);
        FreePalanquee p = new FreePalanquee();
        p.session  = session;
        p.name     = name;
        p.position = (int) count;
        p.persist();
        return p;
    }

    @Transactional
    void assignDiverToPalanquee(Long diverId, Long palanqueeId) {
        FreePalanquee pal   = FreePalanquee.findById(palanqueeId);
        FreeSessionDiver d  = FreeSessionDiver.findById(diverId);
        if (pal == null || d == null) return;
        if (FreePalanqueeMember.findByDiverAndPalanquee(diverId, palanqueeId) == null) {
            FreePalanqueeMember m = new FreePalanqueeMember();
            m.palanquee = pal;
            m.diver     = d;
            m.position  = (int) FreePalanqueeMember.count("palanquee.id", palanqueeId);
            m.persist();
        }
    }

    @Transactional
    void cleanup(Long sessionId) {
        if (sessionId == null) return;
        FreeDiveSession session = FreeDiveSession.findById(sessionId);
        if (session == null) return;
        FreePalanqueeMember.delete("palanquee.session.id", sessionId);
        FreePalanquee.delete("session.id", sessionId);
        FreeSessionDiver.delete("session.id", sessionId);
        FreeSessionDive.delete("session.id", sessionId);
        User owner = session.owner;
        session.delete();
        if (owner != null) owner.delete();
    }

    @Transactional
    void cleanupUser(String email) {
        User u = User.find("email", email).firstResult();
        if (u != null) u.delete();
    }

    // ── CREATE SESSION ────────────────────────────────────────────────────────

    @Test
    @TestSecurity(user = "fs_create@test.com", roles = {"DIVE_DIRECTOR"})
    void createSession_shouldReturn201_withCorrectFields() {
        createDp("fs_create@test.com");
        Long sessionId = null;
        try {
            String body = given()
                .contentType(ContentType.JSON)
                .body("{\"label\":\"Sortie lac\",\"diveDate\":\"2099-07-01\",\"startTime\":\"08:00\"}")
                .when().post(BASE)
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("label", equalTo("Sortie lac"))
                .body("diveDate", equalTo("2099-07-01"))
                .body("startTime", startsWith("08:00"))
                .extract().path("id").toString();
            sessionId = Long.parseLong(body);
        } finally {
            if (sessionId != null) cleanup(sessionId);
            else cleanupUser("fs_create@test.com");
        }
    }

    @Test
    @TestSecurity(user = "fs_create_nolabel@test.com", roles = {"DIVE_DIRECTOR"})
    void createSession_withoutLabel_shouldSucceed() {
        createDp("fs_create_nolabel@test.com");
        Long sessionId = null;
        try {
            String id = given()
                .contentType(ContentType.JSON)
                .body("{\"diveDate\":\"2099-08-01\",\"startTime\":\"10:00\"}")
                .when().post(BASE)
                .then()
                .statusCode(201)
                .body("label", nullValue())
                .extract().path("id").toString();
            sessionId = Long.parseLong(id);
        } finally {
            if (sessionId != null) cleanup(sessionId);
            else cleanupUser("fs_create_nolabel@test.com");
        }
    }

    // ── LIST SESSIONS ─────────────────────────────────────────────────────────

    @Test
    @TestSecurity(user = "fs_list@test.com", roles = {"DIVE_DIRECTOR"})
    void listSessions_shouldReturnOnlyOwnSessions() {
        createDp("fs_list@test.com");
        FreeDiveSession s = createSession("fs_list@test.com");
        try {
            given()
                .when().get(BASE)
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)))
                .body("find { it.id == " + s.id + " }.label", equalTo("Org Test"));
        } finally {
            cleanup(s.id);
        }
    }

    // ── UPDATE SESSION ────────────────────────────────────────────────────────

    @Test
    @TestSecurity(user = "fs_update@test.com", roles = {"DIVE_DIRECTOR"})
    void updateSession_shouldReturn200_withNewValues() {
        createDp("fs_update@test.com");
        FreeDiveSession s = createSession("fs_update@test.com");
        try {
            given()
                .contentType(ContentType.JSON)
                .body("{\"label\":\"Mise à jour\",\"diveDate\":\"2099-09-10\",\"startTime\":\"14:00\"}")
                .when().put(BASE + "/" + s.id)
                .then()
                .statusCode(200)
                .body("label", equalTo("Mise à jour"))
                .body("diveDate", equalTo("2099-09-10"))
                .body("startTime", startsWith("14:00"));
        } finally {
            cleanup(s.id);
        }
    }

    // ── DELETE SESSION ────────────────────────────────────────────────────────

    @Test
    @TestSecurity(user = "fs_delete@test.com", roles = {"DIVE_DIRECTOR"})
    void deleteSession_shouldReturn204_andNoLongerListable() {
        createDp("fs_delete@test.com");
        FreeDiveSession s = createSession("fs_delete@test.com");
        Long id = s.id;
        try {
            given()
                .when().delete(BASE + "/" + id)
                .then()
                .statusCode(204);

            given()
                .when().get(BASE)
                .then()
                .statusCode(200)
                .body("collect { it.id }", not(hasItem(id.intValue())));
        } finally {
            // Déjà supprimé, on nettoie uniquement l'utilisateur
            cleanupUser("fs_delete@test.com");
        }
    }

    // ── LIMITE 15 ─────────────────────────────────────────────────────────────

    @Test
    @TestSecurity(user = "fs_limit@test.com", roles = {"DIVE_DIRECTOR"})
    void createSession_beyondLimit_shouldReturn409() {
        createDp("fs_limit@test.com");
        User owner = User.find("email", "fs_limit@test.com").firstResult();
        // Crée 15 sessions via fixture
        java.util.List<Long> ids = new java.util.ArrayList<>();
        for (int i = 0; i < 15; i++) {
            FreeDiveSession s = createSession("fs_limit@test.com");
            ids.add(s.id);
        }
        try {
            given()
                .contentType(ContentType.JSON)
                .body("{\"diveDate\":\"2099-12-01\",\"startTime\":\"08:00\"}")
                .when().post(BASE)
                .then()
                .statusCode(409);
        } finally {
            for (Long sid : ids) {
                deleteSessionTransactional(sid);
            }
            if (owner != null) deleteUserTransactional(owner.id);
        }
    }

    @Transactional
    void deleteSessionTransactional(Long id) {
        FreeDiveSession s = FreeDiveSession.findById(id);
        if (s == null) return;
        FreePalanqueeMember.delete("palanquee.session.id", id);
        FreePalanquee.delete("session.id", id);
        FreeSessionDiver.delete("session.id", id);
        FreeSessionDive.delete("session.id", id);
        s.delete();
    }

    @Transactional
    void deleteUserTransactional(Long id) {
        User u = User.findById(id);
        if (u != null) u.delete();
    }

    // ── PLONGEURS — ADD ───────────────────────────────────────────────────────

    @Test
    @TestSecurity(user = "fs_diver_add@test.com", roles = {"DIVE_DIRECTOR"})
    void addDiver_shouldReturn201_withNormalizedName() {
        createDp("fs_diver_add@test.com");
        FreeDiveSession s = createSession("fs_diver_add@test.com");
        try {
            given()
                .contentType(ContentType.JSON)
                .body("{\"firstName\":\"jean\",\"lastName\":\"martin\",\"level\":\"Niveau 2\",\"isDirector\":false}")
                .when().post(BASE + "/" + s.id + "/divers")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("firstName", equalTo("Jean"))
                .body("lastName", equalTo("MARTIN"))
                .body("level", equalTo("Niveau 2"));
        } finally {
            cleanup(s.id);
        }
    }

    @Test
    @TestSecurity(user = "fs_diver_dup@test.com", roles = {"DIVE_DIRECTOR"})
    void addDiver_duplicate_shouldReturn409() {
        createDp("fs_diver_dup@test.com");
        FreeDiveSession s = createSession("fs_diver_dup@test.com");
        addDiver(s.id, "DUPONT", "N1");
        try {
            given()
                .contentType(ContentType.JSON)
                .body("{\"firstName\":\"Test\",\"lastName\":\"DUPONT\",\"level\":\"N2\",\"isDirector\":false}")
                .when().post(BASE + "/" + s.id + "/divers")
                .then()
                .statusCode(409);
        } finally {
            cleanup(s.id);
        }
    }

    // ── PLONGEURS — UPDATE ────────────────────────────────────────────────────

    @Test
    @TestSecurity(user = "fs_diver_upd@test.com", roles = {"DIVE_DIRECTOR"})
    void updateDiver_shouldReturn200_withUpdatedLevel() {
        createDp("fs_diver_upd@test.com");
        FreeDiveSession s = createSession("fs_diver_upd@test.com");
        FreeSessionDiver d = addDiver(s.id, "LEBLANC", "Niveau 1");
        try {
            given()
                .contentType(ContentType.JSON)
                .body("{\"firstName\":\"Test\",\"lastName\":\"LEBLANC\",\"level\":\"Niveau 2\",\"isDirector\":false}")
                .when().put(BASE + "/" + s.id + "/divers/" + d.id)
                .then()
                .statusCode(200)
                .body("level", equalTo("Niveau 2"));
        } finally {
            cleanup(s.id);
        }
    }

    // ── PLONGEURS — REMOVE ────────────────────────────────────────────────────

    @Test
    @TestSecurity(user = "fs_diver_del@test.com", roles = {"DIVE_DIRECTOR"})
    void removeDiver_shouldReturn204_andDisappearFromList() {
        createDp("fs_diver_del@test.com");
        FreeDiveSession s = createSession("fs_diver_del@test.com");
        FreeSessionDiver d = addDiver(s.id, "DURAND", "N3");
        try {
            given()
                .when().delete(BASE + "/" + s.id + "/divers/" + d.id)
                .then()
                .statusCode(204);

            given()
                .when().get(BASE + "/" + s.id + "/divers")
                .then()
                .statusCode(200)
                .body("collect { it.id }", not(hasItem(d.id.intValue())));
        } finally {
            cleanup(s.id);
        }
    }

    // ── PLONGÉES — CREATE ─────────────────────────────────────────────────────

    @Test
    @TestSecurity(user = "fs_dive_create@test.com", roles = {"DIVE_DIRECTOR"})
    void createDive_shouldReturn201_withLabel() {
        createDp("fs_dive_create@test.com");
        FreeDiveSession s = createSession("fs_dive_create@test.com");
        try {
            given()
                .contentType(ContentType.JSON)
                .body("{\"label\":\"Plongée 1\",\"startTime\":\"09:00\"}")
                .when().post(BASE + "/" + s.id + "/dives")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("label", equalTo("Plongée 1"))
                .body("diveIndex", equalTo(1));
        } finally {
            cleanup(s.id);
        }
    }

    // ── PLONGÉES — PATCH ──────────────────────────────────────────────────────

    @Test
    @TestSecurity(user = "fs_dive_patch@test.com", roles = {"DIVE_DIRECTOR"})
    void patchDive_shouldUpdateTimes() {
        createDp("fs_dive_patch@test.com");
        FreeDiveSession s = createSession("fs_dive_patch@test.com");
        FreeSessionDive dive = addDive(s.id, "Plongée matin");
        try {
            given()
                .contentType(ContentType.JSON)
                .body("{\"label\":\"Plongée matin\",\"startTime\":\"09:30\",\"endTime\":\"10:30\"}")
                .when().patch(BASE + "/" + s.id + "/dives/" + dive.id)
                .then()
                .statusCode(200)
                .body("startTime", startsWith("09:30"))
                .body("endTime", startsWith("10:30"));
        } finally {
            cleanup(s.id);
        }
    }

    // ── PLONGÉES — DELETE ─────────────────────────────────────────────────────

    @Test
    @TestSecurity(user = "fs_dive_del@test.com", roles = {"DIVE_DIRECTOR"})
    void deleteDive_shouldReturn204() {
        createDp("fs_dive_del@test.com");
        FreeDiveSession s = createSession("fs_dive_del@test.com");
        FreeSessionDive dive = addDive(s.id, "À supprimer");
        try {
            given()
                .when().delete(BASE + "/" + s.id + "/dives/" + dive.id)
                .then()
                .statusCode(204);

            given()
                .when().get(BASE + "/" + s.id + "/dives")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
        } finally {
            cleanup(s.id);
        }
    }

    // ── PALANQUÉES — CREATE ───────────────────────────────────────────────────

    @Test
    @TestSecurity(user = "fs_pal_create@test.com", roles = {"DIVE_DIRECTOR"})
    void createPalanquee_shouldReturn201() {
        createDp("fs_pal_create@test.com");
        FreeDiveSession s = createSession("fs_pal_create@test.com");
        try {
            given()
                .contentType(ContentType.JSON)
                .body("{\"name\":\"P1 Test\"}")
                .when().post(BASE + "/" + s.id + "/palanquees")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("P1 Test"))
                .body("divers", hasSize(0));
        } finally {
            cleanup(s.id);
        }
    }

    // ── PALANQUÉES — UPDATE ───────────────────────────────────────────────────

    @Test
    @TestSecurity(user = "fs_pal_upd@test.com", roles = {"DIVE_DIRECTOR"})
    void updatePalanquee_shouldUpdateNameDepthDuration() {
        createDp("fs_pal_upd@test.com");
        FreeDiveSession s = createSession("fs_pal_upd@test.com");
        FreePalanquee pal = addPalanquee(s.id, "Initiale");
        try {
            given()
                .contentType(ContentType.JSON)
                .body("{\"name\":\"Renommée\",\"depth\":\"30m\",\"duration\":\"50'\"}")
                .when().put(BASE + "/" + s.id + "/palanquees/" + pal.id)
                .then()
                .statusCode(200)
                .body("name", equalTo("Renommée"))
                .body("depth", equalTo("30m"))
                .body("duration", equalTo("50'"));
        } finally {
            cleanup(s.id);
        }
    }

    // ── PALANQUÉES — DELETE ───────────────────────────────────────────────────

    @Test
    @TestSecurity(user = "fs_pal_del@test.com", roles = {"DIVE_DIRECTOR"})
    void deletePalanquee_shouldReturn204_andNotAppearInList() {
        createDp("fs_pal_del@test.com");
        FreeDiveSession s = createSession("fs_pal_del@test.com");
        FreePalanquee pal = addPalanquee(s.id, "À supprimer");
        try {
            given()
                .when().delete(BASE + "/" + s.id + "/palanquees/" + pal.id)
                .then()
                .statusCode(204);

            given()
                .when().get(BASE + "/" + s.id + "/palanquees")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
        } finally {
            cleanup(s.id);
        }
    }

    // ── ASSIGNATION / DÉSASSIGNATION ──────────────────────────────────────────

    @Test
    @TestSecurity(user = "fs_assign@test.com", roles = {"DIVE_DIRECTOR"})
    void assignDiver_shouldAddToPalanquee_thenUnassign() {
        createDp("fs_assign@test.com");
        FreeDiveSession s = createSession("fs_assign@test.com");
        FreeSessionDiver d = addDiver(s.id, "DUPONT", "N2");
        FreePalanquee pal = addPalanquee(s.id, "P1");
        try {
            // Assigner
            given()
                .contentType(ContentType.JSON)
                .body("{\"diverId\":" + d.id + ",\"palanqueeId\":" + pal.id + "}")
                .when().put(BASE + "/" + s.id + "/palanquees/assign")
                .then()
                .statusCode(200);

            given()
                .when().get(BASE + "/" + s.id + "/palanquees")
                .then()
                .statusCode(200)
                .body("[0].divers", hasSize(1))
                .body("[0].divers[0].lastName", equalTo("DUPONT"));

            // Désassigner
            given()
                .contentType(ContentType.JSON)
                .body("{\"diverId\":" + d.id + ",\"palanqueeId\":null}")
                .when().put(BASE + "/" + s.id + "/palanquees/assign")
                .then()
                .statusCode(200);

            given()
                .when().get(BASE + "/" + s.id + "/palanquees")
                .then()
                .statusCode(200)
                .body("[0].divers", hasSize(0));
        } finally {
            cleanup(s.id);
        }
    }

    // ── RÉORDONNANCEMENT ──────────────────────────────────────────────────────

    @Test
    @TestSecurity(user = "fs_reorder@test.com", roles = {"DIVE_DIRECTOR"})
    void reorderPalanquee_shouldReturnMembersInNewOrder() {
        createDp("fs_reorder@test.com");
        FreeDiveSession s = createSession("fs_reorder@test.com");
        FreeSessionDiver d1 = addDiver(s.id, "ALPHA", "N1");
        FreeSessionDiver d2 = addDiver(s.id, "BETA", "N2");
        FreePalanquee pal = addPalanquee(s.id, "P1");
        assignDiverToPalanquee(d1.id, pal.id);
        assignDiverToPalanquee(d2.id, pal.id);
        try {
            // Inverser l'ordre : BETA d'abord, ALPHA ensuite
            given()
                .contentType(ContentType.JSON)
                .body("{\"diverIds\":[" + d2.id + "," + d1.id + "]}")
                .when().put(BASE + "/" + s.id + "/palanquees/" + pal.id + "/reorder")
                .then()
                .statusCode(200);

            given()
                .when().get(BASE + "/" + s.id + "/palanquees")
                .then()
                .statusCode(200)
                .body("find { it.id == " + pal.id + " }.divers[0].lastName", equalTo("BETA"))
                .body("find { it.id == " + pal.id + " }.divers[1].lastName", equalTo("ALPHA"));
        } finally {
            cleanup(s.id);
        }
    }

    // ── APTITUDES SPÉCIFIQUES ─────────────────────────────────────────────────

    @Test
    @TestSecurity(user = "fs_apt@test.com", roles = {"DIVE_DIRECTOR"})
    void updateMemberAptitudes_shouldOverrideForThatPalanquee() {
        createDp("fs_apt@test.com");
        FreeDiveSession s = createSession("fs_apt@test.com");
        FreeSessionDiver d = addDiver(s.id, "MOREAU", "N3");
        FreePalanquee pal = addPalanquee(s.id, "PA");
        assignDiverToPalanquee(d.id, pal.id);
        try {
            given()
                .contentType(ContentType.JSON)
                .body("{\"aptitudes\":\"PE40\"}")
                .when().patch(BASE + "/" + s.id + "/palanquees/" + pal.id + "/members/" + d.id + "/aptitudes")
                .then()
                .statusCode(204);

            given()
                .when().get(BASE + "/" + s.id + "/palanquees")
                .then()
                .statusCode(200)
                .body("find { it.id == " + pal.id + " }.divers.find { it.id == " + d.id + " }.aptitudes", equalTo("PE40"));
        } finally {
            cleanup(s.id);
        }
    }

    // ── ACCÈS REFUSÉ — MAUVAIS PROPRIÉTAIRE ──────────────────────────────────

    @Test
    @TestSecurity(user = "fs_other@test.com", roles = {"DIVE_DIRECTOR"})
    void accessSession_byAnotherDp_shouldReturn403() {
        createDp("fs_other@test.com");
        // Crée une session avec un autre propriétaire (via fixture directe)
        User otherOwner = createDp("fs_owner2@test.com");
        FreeDiveSession s = new FreeDiveSession();
        s.owner     = otherOwner;
        s.diveDate  = LocalDate.of(2099, 11, 1);
        s.startTime = LocalTime.of(9, 0);
        persistSession(s);
        try {
            given()
                .when().get(BASE + "/" + s.id + "/divers")
                .then()
                .statusCode(403);
        } finally {
            cleanup(s.id);
            cleanupUser("fs_other@test.com");
        }
    }

    @Transactional
    void persistSession(FreeDiveSession s) {
        s.persist();
    }

    // ── PLONGÉE ASSIGNÉE À UNE DIVE ───────────────────────────────────────────

    @Test
    @TestSecurity(user = "fs_dive_assign@test.com", roles = {"DIVE_DIRECTOR"})
    void assignPalanqueeToDive_shouldLinkAndUnlink() {
        createDp("fs_dive_assign@test.com");
        FreeDiveSession s = createSession("fs_dive_assign@test.com");
        FreeSessionDive dive = addDive(s.id, "Plongée A");
        FreePalanquee pal = addPalanquee(s.id, "P1");
        try {
            // Assigner palanquee à dive
            given()
                .contentType(ContentType.JSON)
                .body("{\"palanqueeId\":" + pal.id + ",\"diveId\":" + dive.id + "}")
                .when().put(BASE + "/" + s.id + "/dives/assign")
                .then()
                .statusCode(200);

            given()
                .when().get(BASE + "/" + s.id + "/palanquees")
                .then()
                .statusCode(200)
                .body("find { it.id == " + pal.id + " }.diveId", equalTo(dive.id.intValue()));

            // Désassigner
            given()
                .contentType(ContentType.JSON)
                .body("{\"palanqueeId\":" + pal.id + ",\"diveId\":null}")
                .when().put(BASE + "/" + s.id + "/dives/assign")
                .then()
                .statusCode(200);

            given()
                .when().get(BASE + "/" + s.id + "/palanquees")
                .then()
                .statusCode(200)
                .body("find { it.id == " + pal.id + " }.diveId", nullValue());
        } finally {
            cleanup(s.id);
        }
    }
}
