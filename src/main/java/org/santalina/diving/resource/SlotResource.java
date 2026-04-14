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
