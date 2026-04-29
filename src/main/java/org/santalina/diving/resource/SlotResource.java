package org.santalina.diving.resource;

import org.santalina.diving.domain.User;
import org.santalina.diving.domain.UserRole;
import org.santalina.diving.dto.SlotDto.*;
import org.santalina.diving.dto.WaitingListDto.UpdateRegistrationRequest;
import org.santalina.diving.service.SlotService;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Path("/api/slots")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Créneaux de plongée")
public class SlotResource {

    @Inject
    SlotService slotService;

    @Inject
    SecurityIdentity identity;

    @GET
    @PermitAll
    public List<SlotResponse> getByDate(@QueryParam("date") String date) {
        LocalDate localDate = (date != null) ? LocalDate.parse(date) : LocalDate.now();
        return slotService.getSlotsByDate(localDate);
    }

    @GET
    @Path("/week")
    @PermitAll
    public List<SlotResponse> getByWeek(@QueryParam("from") String from) {
        LocalDate fromDate = (from != null) ? LocalDate.parse(from) : LocalDate.now();
        return slotService.getSlotsByWeek(fromDate);
    }

    @GET
    @Path("/month")
    @PermitAll
    public List<SlotResponse> getByMonth(@QueryParam("year") int year, @QueryParam("month") int month) {
        if (year == 0) year = LocalDate.now().getYear();
        if (month == 0) month = LocalDate.now().getMonthValue();
        return slotService.getSlotsByMonth(year, month);
    }

    @GET
    @Path("/{id}")
    @PermitAll
    public SlotResponse getById(@PathParam("id") Long id) {
        return slotService.getById(id);
    }

    @POST
    @RolesAllowed({"ADMIN", "DIVE_DIRECTOR"})
    public Response create(@Valid SlotRequest request) {
        User currentUser = User.findByEmail(identity.getPrincipal().getName());
        if (currentUser == null) throw new NotAuthorizedException("Utilisateur non trouvé");
        User creatorUser = currentUser;
        if (request.createdByUserId() != null && currentUser.roles.contains(UserRole.ADMIN)) {
            User target = User.findById(request.createdByUserId());
            if (target == null) throw new NotFoundException("Directeur de plongée cible non trouvé");
            if (!target.roles.contains(UserRole.DIVE_DIRECTOR))
                throw new BadRequestException("L'utilisateur cible n'est pas directeur de plongée");
            creatorUser = target;
        }
        BatchSlotResponse batch = slotService.createSlots(request, creatorUser);
        return Response.status(201).entity(batch).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "DIVE_DIRECTOR"})
    public Response delete(@PathParam("id") Long id) {
        User currentUser = User.findByEmail(identity.getPrincipal().getName());
        if (currentUser == null) throw new NotAuthorizedException("Utilisateur non trouvé");
        slotService.deleteSlot(id, currentUser);
        return Response.noContent().build();
    }

    @GET
    @Path("/{id}/ics")
    @PermitAll
    @Produces("text/calendar")
    public Response getCalendarIcs(@PathParam("id") Long id) {
        org.santalina.diving.domain.DiveSlot slot = org.santalina.diving.domain.DiveSlot.findById(id);
        if (slot == null) throw new NotFoundException("Créneau non trouvé");

        String uid = "santalina-slot-" + slot.id + "@santalina";
        String dtstamp = LocalDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
        String dateStr = slot.slotDate.format(DateTimeFormatter.BASIC_ISO_DATE);
        String startStr = dateStr + "T" + slot.startTime.format(DateTimeFormatter.ofPattern("HHmmss"));
        String endStr   = dateStr + "T" + slot.endTime.format(DateTimeFormatter.ofPattern("HHmmss"));

        String summary = icsEscape(
                (slot.title != null && !slot.title.isBlank()) ? slot.title : "Plongée");
        String description = slot.notes != null && !slot.notes.isBlank()
                ? "DESCRIPTION:" + icsEscape(slot.notes) + "\r\n" : "";
        String location = slot.club != null && !slot.club.isBlank()
                ? "LOCATION:" + icsEscape(slot.club) + "\r\n" : "";

        String ics = "BEGIN:VCALENDAR\r\n"
                + "VERSION:2.0\r\n"
                + "PRODID:-//Santalina//Santalina Diving//FR\r\n"
                + "CALSCALE:GREGORIAN\r\n"
                + "METHOD:PUBLISH\r\n"
                + "BEGIN:VEVENT\r\n"
                + "UID:" + uid + "\r\n"
                + "DTSTAMP:" + dtstamp + "\r\n"
                + "DTSTART;TZID=Europe/Paris:" + startStr + "\r\n"
                + "DTEND;TZID=Europe/Paris:" + endStr + "\r\n"
                + "SUMMARY:" + summary + "\r\n"
                + description
                + location
                + "END:VEVENT\r\n"
                + "END:VCALENDAR\r\n";

        String filename = "creneau-" + slot.slotDate + ".ics";
        return Response.ok(ics, "text/calendar; charset=utf-8")
                .header("Content-Disposition", "inline; filename=\"" + filename + "\"")
                .build();
    }

    /** Échappe les caractères spéciaux selon la RFC 5545 (ICAL TEXT). */
    private static String icsEscape(String s) {
        return s.replace("\\", "\\\\")
                .replace(",", "\\,")
                .replace(";", "\\;")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n");
    }

    @PATCH
    @Path("/{id}/diver-count")
    @RolesAllowed({"ADMIN", "DIVE_DIRECTOR"})
    public SlotResponse updateDiverCount(@PathParam("id") Long id,
                                         @Valid UpdateDiverCountRequest request) {
        User currentUser = User.findByEmail(identity.getPrincipal().getName());
        if (currentUser == null) throw new NotAuthorizedException("Utilisateur non trouvé");
        return slotService.updateDiverCount(id, request.diverCount(), currentUser);
    }

    @PATCH
    @Path("/{id}/info")
    @RolesAllowed({"ADMIN", "DIVE_DIRECTOR"})
    public SlotResponse updateSlotInfo(@PathParam("id") Long id,
                                       UpdateSlotInfoRequest request) {
        User currentUser = User.findByEmail(identity.getPrincipal().getName());
        if (currentUser == null) throw new NotAuthorizedException("Utilisateur non trouvé");
        return slotService.updateSlotInfo(id, request, currentUser);
    }

    @PATCH
    @Path("/{id}/registration")
    @RolesAllowed({"ADMIN", "DIVE_DIRECTOR"})
    public SlotResponse updateRegistration(@PathParam("id") Long id,
                                           UpdateRegistrationRequest request) {
        User currentUser = User.findByEmail(identity.getPrincipal().getName());
        if (currentUser == null) throw new NotAuthorizedException("Utilisateur non trouvé");
        return slotService.updateRegistration(id, request, currentUser);
    }
}
