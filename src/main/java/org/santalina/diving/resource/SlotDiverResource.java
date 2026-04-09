package org.santalina.diving.resource;

import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.domain.RegistrationStatus;
import org.santalina.diving.domain.SlotDiver;
import org.santalina.diving.domain.User;
import org.santalina.diving.domain.UserRole;
import org.santalina.diving.dto.SlotDiverDto.SlotDiverRequest;
import org.santalina.diving.dto.SlotDiverDto.SlotDiverResponse;
import org.santalina.diving.dto.SlotDiverDto.SlotRegistrationRequest;
import org.santalina.diving.dto.SlotDiverDto.WaitlistEntryResponse;
import org.santalina.diving.mail.BookingNotificationMailer;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Comparator;
import java.util.List;

@Path("/api/slots/{slotId}/divers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Plongeurs")
public class SlotDiverResource {

    @Inject
    JsonWebToken jwt;

    @Inject
    BookingNotificationMailer bookingNotificationMailer;

    // GET /api/slots/{slotId}/divers — public (CONFIRMED uniquement)
    @GET
    @PermitAll
    public List<SlotDiverResponse> getDivers(@PathParam("slotId") Long slotId) {
        DiveSlot slot = DiveSlot.findById(slotId);
        if (slot == null) throw new NotFoundException("Créneau non trouvé");
        return SlotDiver.findConfirmedBySlot(slotId).stream()
                // Directeur en premier, puis ordre alphabétique
                .sorted(Comparator.<SlotDiver, Boolean>comparing(d -> !d.isDirector)
                        .thenComparing(d -> d.lastName))
                .map(SlotDiverResponse::from)
                .toList();
    }

    // POST /api/slots/{slotId}/divers — ADMIN ou DIVE_DIRECTOR
    @POST
    @Transactional
    @RolesAllowed({"ADMIN", "DIVE_DIRECTOR"})
    public Response addDiver(@PathParam("slotId") Long slotId, @Valid SlotDiverRequest request) {
        DiveSlot slot = DiveSlot.findById(slotId);
        if (slot == null) throw new NotFoundException("Créneau non trouvé");

        // Vérifier droits directeur de plongée
        if ("DIVE_DIRECTOR".equals(getRole())) {
            User currentUser = User.findByEmail(jwt.getName());
            if (currentUser == null || slot.createdBy == null || !slot.createdBy.id.equals(currentUser.id)) {
                throw new ForbiddenException("Vous ne pouvez modifier que vos propres créneaux");
            }
        }

        // Validation : champs obligatoires si directeur de plongée
        if (request.isDirector()) {
            if (request.email() == null || request.email().isBlank()) {
                throw new BadRequestException("L'email est obligatoire pour un directeur de plongée");
            }
            if (request.phone() == null || request.phone().isBlank()) {
                throw new BadRequestException("Le téléphone est obligatoire pour un directeur de plongée");
            }
            // Un seul directeur par créneau
            if (SlotDiver.hasDirector(slotId)) {
                throw new BadRequestException("Il y a déjà un directeur de plongée sur ce créneau");
            }
        }

        // Vérifier doublon nom/prénom sur le créneau
        if (SlotDiver.existsBySlotAndName(slotId, request.firstName(), request.lastName())) {
            throw new BadRequestException("Un plongeur avec ce nom et prénom est déjà inscrit sur ce créneau");
        }

        // Vérifier capacité
        long current = SlotDiver.countBySlot(slotId);
        if (current >= slot.diverCount) {
            throw new BadRequestException(
                    "Le créneau est complet (" + slot.diverCount + " plongeurs max)");
        }

        SlotDiver diver = new SlotDiver();
        diver.slot          = slot;
        diver.firstName     = request.firstName().trim();
        diver.lastName      = request.lastName().trim().toUpperCase();
        diver.level         = request.level();
        diver.email         = request.email();
        diver.phone         = request.phone();
        diver.isDirector         = request.isDirector();
        diver.aptitudes          = request.aptitudes();
        diver.licenseNumber      = request.licenseNumber();
        diver.registrationStatus = RegistrationStatus.CONFIRMED;
        diver.numberOfDives      = request.numberOfDives();
        diver.lastDiveDate       = request.lastDiveDate();
        diver.preparedLevel      = request.preparedLevel();
        diver.registrationComment = request.registrationComment();
        // Lier à un compte utilisateur si l'email correspond
        if (diver.email != null && !diver.email.isBlank()) {
            User linked = User.findByEmail(diver.email);
            if (linked != null) diver.registeredUserId = linked.id;
        }
        diver.persist();

        return Response.status(201).entity(SlotDiverResponse.from(diver)).build();
    }

    // PUT /api/slots/{slotId}/divers/{diverId} — ADMIN ou DIVE_DIRECTOR
    @PUT
    @Path("/{diverId}")
    @Transactional
    @RolesAllowed({"ADMIN", "DIVE_DIRECTOR"})
    public SlotDiverResponse updateDiver(@PathParam("slotId") Long slotId,
                                         @PathParam("diverId") Long diverId,
                                         @Valid SlotDiverRequest request) {
        DiveSlot slot = DiveSlot.findById(slotId);
        if (slot == null) throw new NotFoundException("Créneau non trouvé");

        SlotDiver diver = SlotDiver.findById(diverId);
        if (diver == null || !diver.slot.id.equals(slotId)) {
            throw new NotFoundException("Plongeur non trouvé");
        }

        // Vérifier droits directeur de plongée
        String role = getRole();
        if ("DIVE_DIRECTOR".equals(role)) {
            User currentUser = User.findByEmail(jwt.getName());
            if (currentUser == null || slot.createdBy == null || !slot.createdBy.id.equals(currentUser.id)) {
                throw new ForbiddenException("Vous ne pouvez modifier que vos propres créneaux");
            }
        }

        // Validation directeur
        if (request.isDirector()) {
            if (request.email() == null || request.email().isBlank())
                throw new BadRequestException("L'email est obligatoire pour un directeur de plongée");
            if (request.phone() == null || request.phone().isBlank())
                throw new BadRequestException("Le téléphone est obligatoire pour un directeur de plongée");
            // Si on change vers directeur et que ce n'était pas lui avant, vérifier unicité
            if (!diver.isDirector && SlotDiver.hasDirector(slotId))
                throw new BadRequestException("Il y a déjà un directeur de plongée sur ce créneau");
        }

        // Vérifier doublon nom/prénom sur le créneau (en excluant le plongeur courant)
        if (SlotDiver.existsBySlotAndNameExcluding(slotId, request.firstName(), request.lastName(), diverId)) {
            throw new BadRequestException("Un plongeur avec ce nom et prénom est déjà inscrit sur ce créneau");
        }

        diver.firstName     = request.firstName().trim();
        diver.lastName      = request.lastName().trim().toUpperCase();
        diver.level         = request.level();
        diver.email         = request.email();
        diver.phone         = request.phone();
        diver.isDirector    = request.isDirector();
        diver.aptitudes     = request.aptitudes();
        diver.licenseNumber = request.licenseNumber();
        diver.persist();

        return SlotDiverResponse.from(diver);
    }

    // DELETE /api/slots/{slotId}/divers/{diverId} — ADMIN ou DIVE_DIRECTOR
    @DELETE
    @Path("/{diverId}")
    @Transactional
    @RolesAllowed({"ADMIN", "DIVE_DIRECTOR"})
    public Response removeDiver(@PathParam("slotId") Long slotId,
                                @PathParam("diverId") Long diverId) {
        SlotDiver diver = SlotDiver.findById(diverId);
        if (diver == null || !diver.slot.id.equals(slotId)) {
            throw new NotFoundException("Plongeur non trouvé");
        }
        if ("DIVE_DIRECTOR".equals(getRole())) {
            User currentUser = User.findByEmail(jwt.getName());
            if (currentUser == null || diver.slot.createdBy == null || !diver.slot.createdBy.id.equals(currentUser.id)) {
                throw new ForbiddenException("Vous ne pouvez modifier que vos propres créneaux");
            }
        }
        diver.delete();
        return Response.noContent().build();
    }

    // ── POST /api/slots/{slotId}/divers/register — auto-inscription (DIVER / DIVE_DIRECTOR)
    @POST
    @Path("/register")
    @Transactional
    @RolesAllowed({"DIVER", "DIVE_DIRECTOR"})
    public Response registerSelf(@PathParam("slotId") Long slotId,
                                 @Valid SlotRegistrationRequest request) {
        DiveSlot slot = DiveSlot.findById(slotId);
        if (slot == null) throw new NotFoundException("Créneau non trouvé");

        // 1. Inscriptions activées ?
        if (!slot.registrationEnabled) {
            throw new BadRequestException("Les inscriptions sont fermées sur ce créneau");
        }
        // 2. Date d'ouverture respectée ?
        if (slot.registrationOpensAt != null
                && java.time.Instant.now().isBefore(slot.registrationOpensAt)) {
            throw new BadRequestException("Les inscriptions ne sont pas encore ouvertes");
        }
        // 3. Un DP est-il affecté ?
        if (!SlotDiver.hasDirector(slotId)) {
            throw new BadRequestException(
                "Inscription impossible : aucun directeur de plongée n'est affecté à ce créneau");
        }

        User currentUser = getCurrentUser();

        // 4. Déjà inscrit ?
        if (SlotDiver.findBySlotAndRegisteredUser(slotId, currentUser.id) != null) {
            throw new BadRequestException("Vous êtes déjà inscrit (ou en attente) sur ce créneau");
        }

        // 5. Capacité (on compte uniquement les CONFIRMED)
        if (SlotDiver.countConfirmedBySlot(slotId) >= slot.diverCount) {
            throw new BadRequestException(
                "Le créneau est complet (" + slot.diverCount + " places)");
        }

        // 6. Mise à jour de l'email si demandée
        String requestedEmail = request.email().trim().toLowerCase();
        if (!requestedEmail.equalsIgnoreCase(currentUser.email)) {
            User emailOwner = User.findByEmail(requestedEmail);
            if (emailOwner != null && !emailOwner.id.equals(currentUser.id)) {
                throw new BadRequestException("Cet email est déjà utilisé par un autre compte");
            }
            currentUser.email = requestedEmail;
            currentUser.persist();
        }

        SlotDiver diver = new SlotDiver();
        diver.slot               = slot;
        diver.firstName          = currentUser.firstName != null ? currentUser.firstName.trim() : "";
        diver.lastName           = currentUser.lastName  != null ? currentUser.lastName.trim().toUpperCase() : "";
        diver.level              = request.level().trim();
        diver.email              = currentUser.email;
        diver.phone              = currentUser.phone;
        diver.isDirector         = false;
        diver.registrationStatus = RegistrationStatus.PENDING;
        diver.registeredUserId   = currentUser.id;
        diver.numberOfDives      = request.numberOfDives();
        diver.lastDiveDate       = request.lastDiveDate();
        diver.preparedLevel      = request.preparedLevel() != null ? request.preparedLevel().trim() : null;
        diver.registrationComment = request.registrationComment() != null
                ? request.registrationComment().trim() : null;
        diver.persist();

        bookingNotificationMailer.sendRegistrationPendingToApplicant(slot, diver);

        return Response.status(201).entity(SlotDiverResponse.from(diver)).build();
    }

    // ── GET /api/slots/{slotId}/divers/waitlist — file d'attente (DP affecté uniquement)
    @GET
    @Path("/waitlist")
    @RolesAllowed({"DIVE_DIRECTOR", "ADMIN"})
    public List<WaitlistEntryResponse> getWaitlist(@PathParam("slotId") Long slotId) {
        DiveSlot slot = DiveSlot.findById(slotId);
        if (slot == null) throw new NotFoundException("Créneau non trouvé");
        ensureAssignedDirector(slotId);
        return SlotDiver.findPendingBySlot(slotId).stream()
                .map(WaitlistEntryResponse::from)
                .toList();
    }

    // ── POST /api/slots/{slotId}/divers/waitlist/{diverId}/validate — valider une entrée
    @POST
    @Path("/waitlist/{diverId}/validate")
    @Transactional
    @RolesAllowed({"DIVE_DIRECTOR", "ADMIN"})
    public SlotDiverResponse validateRegistration(@PathParam("slotId") Long slotId,
                                                  @PathParam("diverId") Long diverId) {
        DiveSlot slot = DiveSlot.findById(slotId);
        if (slot == null) throw new NotFoundException("Créneau non trouvé");
        ensureAssignedDirector(slotId);

        SlotDiver diver = SlotDiver.findById(diverId);
        if (diver == null || !diver.slot.id.equals(slotId)) throw new NotFoundException("Inscription non trouvée");
        if (diver.registrationStatus != RegistrationStatus.PENDING) {
            throw new BadRequestException("Cette inscription n'est pas en attente de validation");
        }
        // Capacité (CONFIRMED seulement)
        if (SlotDiver.countConfirmedBySlot(slotId) >= slot.diverCount) {
            throw new BadRequestException(
                "Le créneau est complet (" + slot.diverCount + " places), impossible de valider");
        }

        diver.registrationStatus     = RegistrationStatus.CONFIRMED;
        diver.registrationValidatedAt = java.time.LocalDateTime.now();
        diver.persist();

        bookingNotificationMailer.sendRegistrationValidatedToApplicant(slot, diver);

        return SlotDiverResponse.from(diver);
    }

    // ── DELETE /api/slots/{slotId}/divers/registrations/me — annuler sa participation
    @DELETE
    @Path("/registrations/me")
    @Transactional
    @RolesAllowed({"DIVER", "DIVE_DIRECTOR"})
    public Response cancelMyRegistration(@PathParam("slotId") Long slotId) {
        DiveSlot slot = DiveSlot.findById(slotId);
        if (slot == null) throw new NotFoundException("Créneau non trouvé");

        User currentUser = getCurrentUser();
        SlotDiver diver  = SlotDiver.findBySlotAndRegisteredUser(slotId, currentUser.id);
        if (diver == null) throw new NotFoundException("Vous n'êtes pas inscrit sur ce créneau");

        bookingNotificationMailer.sendCancellationToDirector(slot, diver);
        diver.delete();

        return Response.ok(java.util.Map.of("message", "Inscription annulée")).build();
    }

    // ── Helpers privés ────────────────────────────────────────────────────────

    /** Résout l'utilisateur courant depuis le JWT ; lève 401 si introuvable */
    private User getCurrentUser() {
        User u = User.findByEmail(jwt.getName());
        if (u == null) throw new NotAuthorizedException("Utilisateur non trouvé");
        return u;
    }

    /**
     * Vérifie que l'utilisateur courant est le DP affecté au créneau.
     * Un ADMIN peut toujours accéder.
     */
    private void ensureAssignedDirector(Long slotId) {
        String role = getRole();
        if ("ADMIN".equals(role)) return;
        SlotDiver director = SlotDiver.findDirector(slotId);
        if (director == null) throw new ForbiddenException("Aucun directeur affecté à ce créneau");
        User currentUser = getCurrentUser();
        if (director.email == null || !director.email.equalsIgnoreCase(currentUser.email)) {
            throw new ForbiddenException("Seul le directeur de plongée affecté peut gérer la liste d'attente");
        }
    }

    /** Retourne le rôle du JWT courant de façon sécurisée */
    private String getRole() {
        var groups = jwt.getGroups();
        return (groups != null && !groups.isEmpty()) ? groups.iterator().next() : "";
    }
}
