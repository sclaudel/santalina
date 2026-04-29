package org.santalina.diving.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.domain.Palanquee;
import org.santalina.diving.domain.PalanqueeMember;
import org.santalina.diving.domain.SlotDive;
import org.santalina.diving.domain.SlotDiver;
import org.santalina.diving.domain.User;
import org.santalina.diving.domain.UserRole;

import java.time.LocalDate;
import java.time.LocalTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Tests d'intégration pour PalanqueeResource.
 *
 * Couvre :
 *   - CRUD basique des palanquées (create, rename, delete)
 *   - Assignation / désassignation d'un plongeur (assign)
 *   - Mode multi-plongées : un même plongeur assignable à plusieurs palanquées
 *     de plongées différentes (pool partagé)
 *   - Aptitudes spécifiques par plongée (PATCH /members/{diverId}/aptitudes)
 *   - Horaire par plongée reflété dans la réponse (via SlotDive)
 *   - Réordonnancement (reorder)
 */
@QuarkusTest
class PalanqueeResourceIT {

    // ── fixtures ──────────────────────────────────────────────────────────────

    @Transactional
    DiveSlot createSlotWithDp(String dpEmail) {
        User creator = new User();
        creator.email        = dpEmail;
        creator.firstName    = "Dp";
        creator.lastName     = "TEST";
        creator.passwordHash = "x";
        creator.activated    = true;
        creator.role         = UserRole.DIVE_DIRECTOR;
        creator.roles        = java.util.Set.of(UserRole.DIVE_DIRECTOR);
        creator.persist();

        DiveSlot slot = new DiveSlot();
        slot.slotDate   = LocalDate.of(2099, 11, 1);
        slot.startTime  = LocalTime.of(8, 0);
        slot.endTime    = LocalTime.of(17, 0);
        slot.diverCount = 12;
        slot.createdBy  = creator;
        slot.persist();

        SlotDiver dp = new SlotDiver();
        dp.slot       = slot;
        dp.firstName  = "Dp";
        dp.lastName   = "TEST";
        dp.level      = "MF1";
        dp.email      = dpEmail;
        dp.isDirector = true;
        dp.persist();

        return slot;
    }

    @Transactional
    SlotDiver addDiver(Long slotId, String lastName, String level) {
        DiveSlot slot = DiveSlot.findById(slotId);
        SlotDiver d = new SlotDiver();
        d.slot       = slot;
        d.firstName  = "Test";
        d.lastName   = lastName;
        d.level      = level;
        d.isDirector = false;
        d.persist();
        return d;
    }

    @Transactional
    SlotDive createDive(Long slotId, String label, String start, String end) {
        DiveSlot slot = DiveSlot.findById(slotId);
        SlotDive dive = new SlotDive();
        dive.slot      = slot;
        dive.diveIndex = (int) SlotDive.count("slot.id", slotId) + 1;
        dive.label     = label;
        if (start != null) dive.startTime = LocalTime.parse(start);
        if (end   != null) dive.endTime   = LocalTime.parse(end);
        dive.persist();
        return dive;
    }

    @Transactional
    Long createPalanquee(Long slotId, String name) {
        DiveSlot slot = DiveSlot.findById(slotId);
        long count = Palanquee.count("slot.id", slotId);
        Palanquee p = new Palanquee();
        p.slot     = slot;
        p.name     = name;
        p.position = (int) count;
        p.persist();
        return p.id;
    }

    @Transactional
    void assignDiverToPalanquee(Long slotId, Long diverId, Long palanqueeId) {
        Palanquee pal  = Palanquee.findById(palanqueeId);
        SlotDiver diver = SlotDiver.findById(diverId);
        if (pal == null || diver == null) return;
        if (PalanqueeMember.findByDiverAndPalanquee(diverId, palanqueeId) == null) {
            PalanqueeMember m = new PalanqueeMember();
            m.palanquee = pal;
            m.diver     = diver;
            m.position  = (int) PalanqueeMember.count("palanquee.id = ?1", palanqueeId);
            m.persist();
        }
    }

    @Transactional
    void cleanup(Long slotId) {
        if (slotId == null) return;
        DiveSlot slot = DiveSlot.findById(slotId);
        if (slot == null) return;
        // Les PalanqueeMembers sont supprimés en cascad via palanquees
        Palanquee.delete("slot", slot);
        SlotDiver.delete("slot", slot);
        SlotDive.delete("slot", slot);
        if (slot.createdBy != null) slot.createdBy.delete();
        slot.delete();
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Test
    @TestSecurity(user = "dp_pal_create@test.com", roles = {"DIVE_DIRECTOR"})
    void createPalanquee_shouldReturn201_withName() {
        DiveSlot slot = createSlotWithDp("dp_pal_create@test.com");
        try {
            given()
                .contentType(ContentType.JSON)
                .body("{\"name\":\"Palanquée test\"}")
                .when().post("/api/slots/" + slot.id + "/palanquees")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("Palanquée test"))
                .body("divers", hasSize(0));
        } finally {
            cleanup(slot.id);
        }
    }

    // ── RENAME ────────────────────────────────────────────────────────────────

    @Test
    @TestSecurity(user = "dp_pal_rename@test.com", roles = {"DIVE_DIRECTOR"})
    void renamePalanquee_shouldUpdateNameDepthDuration() {
        DiveSlot slot = createSlotWithDp("dp_pal_rename@test.com");
        Long palId = createPalanquee(slot.id, "Initiale");
        try {
            given()
                .contentType(ContentType.JSON)
                .body("{\"name\":\"Renommée\",\"depth\":\"20m\",\"duration\":\"40'\"}")
                .when().put("/api/slots/" + slot.id + "/palanquees/" + palId)
                .then()
                .statusCode(200)
                .body("name", equalTo("Renommée"))
                .body("depth", equalTo("20m"))
                .body("duration", equalTo("40'"));
        } finally {
            cleanup(slot.id);
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Test
    @TestSecurity(user = "dp_pal_delete@test.com", roles = {"DIVE_DIRECTOR"})
    void deletePalanquee_shouldReturn204_andNoLongerAppearInList() {
        DiveSlot slot = createSlotWithDp("dp_pal_delete@test.com");
        Long palId = createPalanquee(slot.id, "À supprimer");
        try {
            given()
                .when().delete("/api/slots/" + slot.id + "/palanquees/" + palId)
                .then()
                .statusCode(204);

            given()
                .when().get("/api/slots/" + slot.id + "/palanquees")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
        } finally {
            cleanup(slot.id);
        }
    }

    // ── ASSIGN / UNASSIGN ─────────────────────────────────────────────────────

    @Test
    @TestSecurity(user = "dp_pal_assign@test.com", roles = {"DIVE_DIRECTOR"})
    void assignDiver_shouldAddDiverToPalanquee_thenUnassign() {
        DiveSlot slot  = createSlotWithDp("dp_pal_assign@test.com");
        SlotDiver d    = addDiver(slot.id, "DURAND", "N2");
        Long palId     = createPalanquee(slot.id, "P1");
        try {
            // Assigner
            given()
                .contentType(ContentType.JSON)
                .body("{\"diverId\":" + d.id + ",\"palanqueeId\":" + palId + "}")
                .when().put("/api/slots/" + slot.id + "/palanquees/assign")
                .then()
                .statusCode(200);

            given()
                .when().get("/api/slots/" + slot.id + "/palanquees")
                .then()
                .statusCode(200)
                .body("[0].divers", hasSize(1))
                .body("[0].divers[0].lastName", equalTo("DURAND"));

            // Désassigner (toutes les palanquées)
            given()
                .contentType(ContentType.JSON)
                .body("{\"diverId\":" + d.id + ",\"palanqueeId\":null}")
                .when().put("/api/slots/" + slot.id + "/palanquees/assign")
                .then()
                .statusCode(200);

            given()
                .when().get("/api/slots/" + slot.id + "/palanquees")
                .then()
                .statusCode(200)
                .body("[0].divers", hasSize(0));
        } finally {
            cleanup(slot.id);
        }
    }

    // ── MULTI-PLONGÉES : pool partagé ─────────────────────────────────────────

    @Test
    @TestSecurity(user = "dp_multipool@test.com", roles = {"DIVE_DIRECTOR"})
    void sameDiver_canBeAssigned_toTwoPalanquees_inDifferentDives() {
        DiveSlot slot   = createSlotWithDp("dp_multipool@test.com");
        SlotDiver diver = addDiver(slot.id, "MARTIN", "N3");
        Long pal1       = createPalanquee(slot.id, "P-Matin");
        Long pal2       = createPalanquee(slot.id, "P-Apres-midi");
        try {
            // Assigner dans P1
            given()
                .contentType(ContentType.JSON)
                .body("{\"diverId\":" + diver.id + ",\"palanqueeId\":" + pal1 + "}")
                .when().put("/api/slots/" + slot.id + "/palanquees/assign")
                .then()
                .statusCode(200);

            // Assigner dans P2 également (multi-plongées)
            given()
                .contentType(ContentType.JSON)
                .body("{\"diverId\":" + diver.id + ",\"palanqueeId\":" + pal2 + "}")
                .when().put("/api/slots/" + slot.id + "/palanquees/assign")
                .then()
                .statusCode(200);

            // Les deux palanquées contiennent bien le plongeur
            given()
                .when().get("/api/slots/" + slot.id + "/palanquees")
                .then()
                .statusCode(200)
                .body("[0].divers", hasSize(1))
                .body("[1].divers", hasSize(1));

            // Désassigner uniquement de P1 (fromPalanqueeId)
            given()
                .contentType(ContentType.JSON)
                .body("{\"diverId\":" + diver.id + ",\"palanqueeId\":null,\"fromPalanqueeId\":" + pal1 + "}")
                .when().put("/api/slots/" + slot.id + "/palanquees/assign")
                .then()
                .statusCode(200);

            // P1 vide, P2 toujours avec le plongeur
            given()
                .when().get("/api/slots/" + slot.id + "/palanquees")
                .then()
                .statusCode(200)
                .body("find { it.id == " + pal1 + " }.divers", hasSize(0))
                .body("find { it.id == " + pal2 + " }.divers", hasSize(1));
        } finally {
            cleanup(slot.id);
        }
    }

    // ── APTITUDES PAR PLONGÉE ─────────────────────────────────────────────────

    @Test
    @TestSecurity(user = "dp_aptitudes@test.com", roles = {"DIVE_DIRECTOR"})
    void memberAptitudes_shouldOverrideGlobalAptitudes_inPalanqueeResponse() {
        DiveSlot slot   = createSlotWithDp("dp_aptitudes@test.com");
        SlotDiver diver = addDiver(slot.id, "DUPONT", "N2");
        Long palId      = createPalanquee(slot.id, "P1");
        assignDiverToPalanquee(slot.id, diver.id, palId);
        try {
            // Sans aptitudes → retourne les aptitudes globales du plongeur (null ici)
            given()
                .when().get("/api/slots/" + slot.id + "/palanquees")
                .then()
                .statusCode(200)
                .body("[0].divers[0].aptitudes", nullValue());

            // Définir des aptitudes spécifiques à cette plongée
            given()
                .contentType(ContentType.JSON)
                .body("{\"aptitudes\":\"PE40\"}")
                .when().patch("/api/slots/" + slot.id + "/palanquees/" + palId + "/members/" + diver.id + "/aptitudes")
                .then()
                .statusCode(204);

            // La palanquée retourne maintenant PE40 pour ce plongeur
            given()
                .when().get("/api/slots/" + slot.id + "/palanquees")
                .then()
                .statusCode(200)
                .body("[0].divers[0].aptitudes", equalTo("PE40"));

            // Effacer les aptitudes spécifiques (null → retour aux aptitudes globales)
            given()
                .contentType(ContentType.JSON)
                .body("{\"aptitudes\":null}")
                .when().patch("/api/slots/" + slot.id + "/palanquees/" + palId + "/members/" + diver.id + "/aptitudes")
                .then()
                .statusCode(204);

            given()
                .when().get("/api/slots/" + slot.id + "/palanquees")
                .then()
                .statusCode(200)
                .body("[0].divers[0].aptitudes", nullValue());
        } finally {
            cleanup(slot.id);
        }
    }

    @Test
    @TestSecurity(user = "dp_apt_different@test.com", roles = {"DIVE_DIRECTOR"})
    void sameDiver_canHave_differentAptitudes_inDifferentPalanquees() {
        DiveSlot slot   = createSlotWithDp("dp_apt_different@test.com");
        SlotDiver diver = addDiver(slot.id, "BERNARD", "N3");
        Long pal1       = createPalanquee(slot.id, "Matin");
        Long pal2       = createPalanquee(slot.id, "Après-midi");
        assignDiverToPalanquee(slot.id, diver.id, pal1);
        assignDiverToPalanquee(slot.id, diver.id, pal2);
        try {
            // Aptitudes différentes sur chaque plongée
            given()
                .contentType(ContentType.JSON)
                .body("{\"aptitudes\":\"PE20\"}")
                .when().patch("/api/slots/" + slot.id + "/palanquees/" + pal1 + "/members/" + diver.id + "/aptitudes")
                .then()
                .statusCode(204);

            given()
                .contentType(ContentType.JSON)
                .body("{\"aptitudes\":\"PA40\"}")
                .when().patch("/api/slots/" + slot.id + "/palanquees/" + pal2 + "/members/" + diver.id + "/aptitudes")
                .then()
                .statusCode(204);

            // Vérifier que chaque palanquée retourne l'aptitude spécifique
            given()
                .when().get("/api/slots/" + slot.id + "/palanquees")
                .then()
                .statusCode(200)
                .body("find { it.id == " + pal1 + " }.divers[0].aptitudes", equalTo("PE20"))
                .body("find { it.id == " + pal2 + " }.divers[0].aptitudes", equalTo("PA40"));
        } finally {
            cleanup(slot.id);
        }
    }

    @Test
    @TestSecurity(user = "dp_apt_notfound@test.com", roles = {"DIVE_DIRECTOR"})
    void memberAptitudes_shouldReturn404_whenMemberDoesNotExist() {
        DiveSlot slot = createSlotWithDp("dp_apt_notfound@test.com");
        Long palId    = createPalanquee(slot.id, "P1");
        try {
            given()
                .contentType(ContentType.JSON)
                .body("{\"aptitudes\":\"PE40\"}")
                .when().patch("/api/slots/" + slot.id + "/palanquees/" + palId + "/members/99999999/aptitudes")
                .then()
                .statusCode(404);
        } finally {
            cleanup(slot.id);
        }
    }

    // ── HORAIRE PAR PLONGÉE ───────────────────────────────────────────────────

    @Test
    @TestSecurity(user = "dp_divetime@test.com", roles = {"DIVE_DIRECTOR"})
    void createDiveWithTime_shouldPersistStartEndTime() {
        DiveSlot slot = createSlotWithDp("dp_divetime@test.com");
        try {
            given()
                .contentType(ContentType.JSON)
                .body("{\"label\":\"Matin\",\"startTime\":\"09:00\",\"endTime\":\"12:30\"}")
                .when().post("/api/slots/" + slot.id + "/dives")
                .then()
                .statusCode(201)
                .body("startTime", equalTo("09:00:00"))
                .body("endTime",   equalTo("12:30:00"));
        } finally {
            cleanup(slot.id);
        }
    }

    @Test
    @TestSecurity(user = "dp_divetime_patch@test.com", roles = {"DIVE_DIRECTOR"})
    void updateDiveTime_shouldPersistAndReturnUpdatedTime() {
        DiveSlot slot = createSlotWithDp("dp_divetime_patch@test.com");
        SlotDive dive = createDive(slot.id, "Matin", "09:00", "12:00");
        try {
            given()
                .contentType(ContentType.JSON)
                .body("{\"label\":\"Matin\",\"startTime\":\"10:00\",\"endTime\":\"13:00\"}")
                .when().patch("/api/slots/" + slot.id + "/dives/" + dive.id)
                .then()
                .statusCode(200)
                .body("startTime", equalTo("10:00:00"))
                .body("endTime",   equalTo("13:00:00"));
        } finally {
            cleanup(slot.id);
        }
    }

    // ── REORDER ───────────────────────────────────────────────────────────────

    @Test
    @TestSecurity(user = "dp_reorder@test.com", roles = {"DIVE_DIRECTOR"})
    void reorderDivers_shouldUpdatePositionOrder() {
        DiveSlot slot   = createSlotWithDp("dp_reorder@test.com");
        SlotDiver d1    = addDiver(slot.id, "ALPHA", "N2");
        SlotDiver d2    = addDiver(slot.id, "BETA",  "N3");
        Long palId      = createPalanquee(slot.id, "P1");
        assignDiverToPalanquee(slot.id, d1.id, palId);
        assignDiverToPalanquee(slot.id, d2.id, palId);
        try {
            // Réordonner : BETA d'abord, ALPHA ensuite
            given()
                .contentType(ContentType.JSON)
                .body("{\"diverIds\":[" + d2.id + "," + d1.id + "]}")
                .when().put("/api/slots/" + slot.id + "/palanquees/" + palId + "/reorder")
                .then()
                .statusCode(200);

            // Vérifier l'ordre retourné
            given()
                .when().get("/api/slots/" + slot.id + "/palanquees")
                .then()
                .statusCode(200)
                .body("[0].divers[0].lastName", equalTo("BETA"))
                .body("[0].divers[1].lastName", equalTo("ALPHA"));
        } finally {
            cleanup(slot.id);
        }
    }

    // ── ACCÈS NON AUTORISÉ ──────────────────────────────────────────────────

    @Test
    void createPalanquee_shouldReturn401_withoutAuthentication() {
        DiveSlot slot = createSlotWithDp("dp_pal_401_" + System.nanoTime() + "@test.com");
        try {
            given()
                .contentType(ContentType.JSON)
                .body("{\"name\":\"test\"}")
                .when().post("/api/slots/" + slot.id + "/palanquees")
                .then()
                .statusCode(401);
        } finally {
            cleanup(slot.id);
        }
    }

    @Test
    @TestSecurity(user = "diver_pal@test.com", roles = {"DIVER"})
    void createPalanquee_shouldReturn403_forDiver() {
        DiveSlot slot = createSlotWithDp("dp_pal_403_" + System.nanoTime() + "@test.com");
        try {
            given()
                .contentType(ContentType.JSON)
                .body("{\"name\":\"test\"}")
                .when().post("/api/slots/" + slot.id + "/palanquees")
                .then()
                .statusCode(403);
        } finally {
            cleanup(slot.id);
        }
    }
}
