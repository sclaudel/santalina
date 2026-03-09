package org.santalina.diving.resource;

import org.santalina.diving.dto.ConfigDto.*;
import org.santalina.diving.service.ConfigService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Configuration")
public class ConfigResource {

    @Inject
    ConfigService configService;

    @GET
    @PermitAll
    public ConfigResponse getConfig() {
        return configService.getConfig();
    }

    @PUT
    @Path("/max-divers")
    @RolesAllowed("ADMIN")
    public ConfigResponse updateMaxDivers(@Valid UpdateMaxDiversRequest request) {
        return configService.updateMaxDivers(request.maxDivers());
    }

    @PUT
    @Path("/site-name")
    @RolesAllowed("ADMIN")
    public ConfigResponse updateSiteName(@Valid UpdateSiteNameRequest request) {
        return configService.updateSiteName(request.siteName());
    }

    @PUT
    @Path("/slot-types")
    @RolesAllowed("ADMIN")
    public ConfigResponse updateSlotTypes(@Valid UpdateListRequest request) {
        return configService.updateSlotTypes(request.items());
    }

    @PUT
    @Path("/clubs")
    @RolesAllowed("ADMIN")
    public ConfigResponse updateClubs(@Valid UpdateListRequest request) {
        return configService.updateClubs(request.items());
    }

    @PUT
    @Path("/levels")
    @RolesAllowed("ADMIN")
    public ConfigResponse updateLevels(@Valid UpdateListRequest request) {
        return configService.updateLevels(request.items());
    }

    @PUT
    @Path("/public-access")
    @RolesAllowed("ADMIN")
    public ConfigResponse updatePublicAccess(@Valid UpdateBooleanRequest request) {
        return configService.updatePublicAccess(request.value());
    }

    @PUT
    @Path("/self-registration")
    @RolesAllowed("ADMIN")
    public ConfigResponse updateSelfRegistration(@Valid UpdateBooleanRequest request) {
        return configService.updateSelfRegistration(request.value());
    }
}
