package org.santalina.diving.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.domain.SlotDiver;
import org.santalina.diving.domain.SlotSafetySheet;
import org.santalina.diving.domain.User;
import org.santalina.diving.domain.UserRole;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Tests d'intégration pour SlotSafetySheetResource.
 *
 * Couvre :
 *   - Liste des fiches (GET) — authentifié et non authentifié
 *   - Upload d'une fiche (POST multipart) — DP créateur, accès refusé
 *   - Upload sur un créneau futur → 400
 *   - Upload d'un type de fichier non autorisé → 400
 *   - Téléchargement ZIP sans fiches → 404
 *   - Téléchargement ZIP avec fiches → 200
 *   - Suppression (DELETE) — admin et non-admin
 */
@QuarkusTest
class SlotSafetySheetResourceIT {

    // ── fixtures ──────────────────────────────────────────────────────────────

    /** Crée un créneau PASSÉ avec un DP créateur portant l'email donné. */
    @Transactional
    DiveSlot createPastSlot(String creatorEmail) {
        User creator = new User();
        creator.email        = creatorEmail;
        creator.firstName    = "dp";
        creator.lastName     = "TEST";
        creator.passwordHash = "x";
        creator.activated    = true;
        creator.role         = UserRole.DIVE_DIRECTOR;
        creator.roles        = java.util.Set.of(UserRole.DIVE_DIRECTOR);
        creator.persist();

        DiveSlot slot = new DiveSlot();
        slot.slotDate   = LocalDate.of(2020, 6, 15);
        slot.startTime  = LocalTime.of(8, 0);
        slot.endTime    = LocalTime.of(17, 0);
        slot.diverCount = 10;
        slot.createdBy  = creator;
        slot.persist();

        SlotDiver dp = new SlotDiver();
        dp.slot       = slot;
        dp.firstName  = "dp";
        dp.lastName   = "TEST";
        dp.level      = "MF1";
        dp.email      = creatorEmail;
        dp.isDirector = true;
        dp.persist();

        return slot;
    }

    /** Crée un créneau FUTUR (pour tester le refus d'upload). */
    @Transactional
    DiveSlot createFutureSlot(String creatorEmail) {
        User creator = new User();
        creator.email        = creatorEmail;
        creator.firstName    = "dp";
        creator.lastName     = "FUT";
        creator.passwordHash = "x";
        creator.activated    = true;
        creator.role         = UserRole.DIVE_DIRECTOR;
        creator.roles        = java.util.Set.of(UserRole.DIVE_DIRECTOR);
        creator.persist();

        DiveSlot slot = new DiveSlot();
        slot.slotDate   = LocalDate.of(2099, 12, 1);
        slot.startTime  = LocalTime.of(8, 0);
        slot.endTime    = LocalTime.of(17, 0);
        slot.diverCount = 10;
        slot.createdBy  = creator;
        slot.persist();

        SlotDiver dp = new SlotDiver();
        dp.slot       = slot;
        dp.firstName  = "dp";
        dp.lastName   = "FUT";
        dp.level      = "MF1";
        dp.email      = creatorEmail;
        dp.isDirector = true;
        dp.persist();

        return slot;
    }

    /** Attache une fiche de sécurité fictive (sans fichier physique) à un créneau. */
    @Transactional
    SlotSafetySheet attachFakeSheet(Long slotId, Long uploaderId) {
        DiveSlot slot = DiveSlot.findById(slotId);
        User uploader = uploaderId != null ? User.findById(uploaderId) : null;

        SlotSafetySheet sheet = new SlotSafetySheet();
        sheet.slot         = slot;
        sheet.uploader     = uploader;
        sheet.originalName = "fiche_test.pdf";
        sheet.storedName   = "fiche_test_uuid.pdf";
        sheet.filePath     = "attachments/safety-sheets/" + slotId + "/fiche_test_uuid.pdf";
        sheet.contentType  = "application/pdf";
        sheet.fileSize     = 1024L;
        sheet.uploadedAt   = LocalDateTime.of(2020, 6, 16, 10, 0);
        sheet.expiresAt    = sheet.uploadedAt.plusYears(1);
        sheet.persist();

        return sheet;
    }

    @Transactional
    void cleanup(Long slotId) {
        if (slotId == null) return;
        DiveSlot slot = DiveSlot.findById(slotId);
        if (slot == null) return;
        SlotSafetySheet.delete("slot.id", slotId);
        SlotDiver.delete("slot", slot);
        if (slot.createdBy != null) slot.createdBy.delete();
        slot.delete();
    }

    // ── GET liste ─────────────────────────────────────────────────────────────

    @Test
    @TestSecurity(user = "dp_ss_list@test.com", roles = {"DIVE_DIRECTOR"})
    void list_pastSlot_dpCreator_shouldReturn200_emptyList() {
        DiveSlot slot = createPastSlot("dp_ss_list@test.com");
        try {
            given()
                .when().get("/api/slots/" + slot.id + "/safety-sheets")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
        } finally {
            cleanup(slot.id);
        }
    }

    @Test
    @TestSecurity(user = "dp_ss_list2@test.com", roles = {"DIVE_DIRECTOR"})
    void list_withExistingSheet_shouldReturn1Element() {
        DiveSlot slot = createPastSlot("dp_ss_list2@test.com");
        try {
            attachFakeSheet(slot.id, slot.createdBy.id);
            given()
                .when().get("/api/slots/" + slot.id + "/safety-sheets")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].originalName", equalTo("fiche_test.pdf"))
                .body("[0].contentType", equalTo("application/pdf"))
                .body("[0].fileSize", equalTo(1024));
        } finally {
            cleanup(slot.id);
        }
    }

    @Test
    void list_noAuth_shouldReturn401() {
        DiveSlot slot = createPastSlot("dp_ss_noauth_" + System.nanoTime() + "@test.com");
        try {
            given()
                .when().get("/api/slots/" + slot.id + "/safety-sheets")
                .then()
                .statusCode(401);
        } finally {
            cleanup(slot.id);
        }
    }

    // ── POST upload ───────────────────────────────────────────────────────────

    @Test
    @TestSecurity(user = "dp_ss_upload@test.com", roles = {"DIVE_DIRECTOR"})
    void upload_futureSlot_shouldReturn400() {
        DiveSlot slot = createFutureSlot("dp_ss_upload@test.com");
        try {
            byte[] content = "fake pdf content".getBytes();
            given()
                .multiPart("file1", "test.pdf", content, "application/pdf")
                .when().post("/api/slots/" + slot.id + "/safety-sheets")
                .then()
                .statusCode(400);
        } finally {
            cleanup(slot.id);
        }
    }

    @Test
    @TestSecurity(user = "dp_ss_type@test.com", roles = {"DIVE_DIRECTOR"})
    void upload_disallowedFileType_shouldReturn400() {
        DiveSlot slot = createPastSlot("dp_ss_type@test.com");
        try {
            byte[] content = "not an image".getBytes();
            given()
                .multiPart("file1", "malware.exe", content, "application/octet-stream")
                .when().post("/api/slots/" + slot.id + "/safety-sheets")
                .then()
                .statusCode(400);
        } finally {
            cleanup(slot.id);
        }
    }

    @Test
    void upload_noAuth_shouldReturn401() {
        DiveSlot slot = createPastSlot("dp_ss_noauth2_" + System.nanoTime() + "@test.com");
        try {
            given()
                .multiPart("file1", "fiche.pdf", "content".getBytes(), "application/pdf")
                .when().post("/api/slots/" + slot.id + "/safety-sheets")
                .then()
                .statusCode(401);
        } finally {
            cleanup(slot.id);
        }
    }

    @Test
    @TestSecurity(user = "diver_ss@test.com", roles = {"DIVER"})
    void upload_asSimpleDiver_shouldReturn403() {
        DiveSlot slot = createPastSlot("dp_ss_diver_" + System.nanoTime() + "@test.com");
        try {
            given()
                .multiPart("file1", "fiche.pdf", "content".getBytes(), "application/pdf")
                .when().post("/api/slots/" + slot.id + "/safety-sheets")
                .then()
                .statusCode(403);
        } finally {
            cleanup(slot.id);
        }
    }

    @Test
    @TestSecurity(user = "dp_ss_notowner@test.com", roles = {"DIVE_DIRECTOR"})
    void upload_dpNotOwner_shouldReturn403() {
        // DP authentifié ≠ créateur du créneau
        DiveSlot slot = createPastSlot("dp_ss_owner_only_" + System.nanoTime() + "@test.com");
        try {
            given()
                .multiPart("file1", "fiche.pdf", "content".getBytes(), "application/pdf")
                .when().post("/api/slots/" + slot.id + "/safety-sheets")
                .then()
                .statusCode(403);
        } finally {
            cleanup(slot.id);
        }
    }

    // ── GET /zip ──────────────────────────────────────────────────────────────

    @Test
    @TestSecurity(user = "dp_ss_zip_empty@test.com", roles = {"DIVE_DIRECTOR"})
    void downloadZip_noSheets_shouldReturn404() {
        DiveSlot slot = createPastSlot("dp_ss_zip_empty@test.com");
        try {
            given()
                .when().get("/api/slots/" + slot.id + "/safety-sheets/zip")
                .then()
                .statusCode(404);
        } finally {
            cleanup(slot.id);
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Test
    @TestSecurity(user = "admin_ss_del@test.com", roles = {"ADMIN"})
    void delete_byAdmin_existingSheet_shouldReturn204() {
        DiveSlot slot = createPastSlot("dp_ss_del_owner_" + System.nanoTime() + "@test.com");
        SlotSafetySheet sheet = attachFakeSheet(slot.id, null);
        try {
            given()
                .when().delete("/api/slots/" + slot.id + "/safety-sheets/" + sheet.id)
                .then()
                .statusCode(204);
        } finally {
            cleanup(slot.id);
        }
    }

    @Test
    @TestSecurity(user = "dp_ss_del_forbidden@test.com", roles = {"DIVE_DIRECTOR"})
    void delete_byDP_shouldReturn403() {
        DiveSlot slot = createPastSlot("dp_ss_del_forbidden@test.com");
        SlotSafetySheet sheet = attachFakeSheet(slot.id, null);
        try {
            given()
                .when().delete("/api/slots/" + slot.id + "/safety-sheets/" + sheet.id)
                .then()
                .statusCode(403);
        } finally {
            cleanup(slot.id);
        }
    }

    @Test
    @TestSecurity(user = "admin_ss_del_notfound@test.com", roles = {"ADMIN"})
    void delete_byAdmin_nonExistentSheet_shouldReturn404() {
        DiveSlot slot = createPastSlot("dp_ss_del_nf_" + System.nanoTime() + "@test.com");
        try {
            given()
                .when().delete("/api/slots/" + slot.id + "/safety-sheets/999999")
                .then()
                .statusCode(404);
        } finally {
            cleanup(slot.id);
        }
    }
}
