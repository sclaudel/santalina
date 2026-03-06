package com.example.diving.resource;

import com.example.diving.domain.User;
import com.example.diving.domain.UserRole;
import com.example.diving.dto.SlotDto.*;
import com.example.diving.service.SlotService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
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
    JsonWebToken jwt;

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
    @Path("/{id}")
    @PermitAll
    public SlotResponse getById(@PathParam("id") Long id) {
        return slotService.getById(id);
    }

    @POST
    @RolesAllowed({"ADMIN", "DIVE_DIRECTOR"})
    public Response create(@Valid SlotRequest request) {
        User currentUser = User.findByEmail(jwt.getName());
        if (currentUser == null) throw new NotAuthorizedException("Utilisateur non trouvé");
        SlotResponse slot = slotService.createSlot(request, currentUser);
        return Response.status(201).entity(slot).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "DIVE_DIRECTOR"})
    public Response delete(@PathParam("id") Long id) {
        User currentUser = User.findByEmail(jwt.getName());
        if (currentUser == null) throw new NotAuthorizedException("Utilisateur non trouvé");
        slotService.deleteSlot(id, currentUser);
        return Response.noContent().build();
    }
}

