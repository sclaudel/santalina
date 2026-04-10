package org.santalina.diving.resource;

import org.santalina.diving.dto.UserDto.*;
import org.santalina.diving.dto.AuthDto.LoginResponse;
import org.santalina.diving.service.UserService;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Utilisateurs")
public class UserResource {

    @Inject
    UserService userService;

    @Inject
    JsonWebToken jwt;

    @Inject
    SecurityIdentity identity;

    @GET
    @Path("/me")
    @RolesAllowed({"ADMIN", "DIVE_DIRECTOR", "DIVER"})
    public UserResponse getProfile() {
        return userService.getProfile(jwt.getName());
    }

    @PUT
    @Path("/me")
    @RolesAllowed({"ADMIN", "DIVE_DIRECTOR", "DIVER"})
    public UserResponse updateProfile(@Valid UpdateProfileRequest request) {
        return userService.updateProfile(jwt.getName(), request);
    }

    @PATCH
    @Path("/me/email")
    @RolesAllowed({"ADMIN", "DIVE_DIRECTOR", "DIVER"})
    public LoginResponse updateEmail(@Valid UpdateEmailRequest request) {
        return userService.updateEmail(jwt.getName(), request);
    }

    @GET
    @RolesAllowed("ADMIN")
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @GET
    @Path("/search")
    @RolesAllowed({"ADMIN", "DIVE_DIRECTOR"})
    public List<UserSearchResult> searchUsers(@QueryParam("q") String q) {
        return userService.searchUsers(q);
    }

    @POST
    @RolesAllowed("ADMIN")
    public Response createUser(@Valid CreateUserRequest request) {
        UserResponse created = userService.createUser(request);
        return Response.status(201).entity(created).build();
    }

    @PUT
    @Path("/{id}/roles")
    @RolesAllowed("ADMIN")
    public UserResponse updateRoles(@PathParam("id") Long id, @Valid UpdateRolesRequest request) {
        return userService.updateRoles(id, request);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public Response deleteUser(@PathParam("id") Long id) {
        userService.deleteUser(id);
        return Response.noContent().build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public UserResponse updateUserAsAdmin(@PathParam("id") Long id, @Valid UpdateUserAdminRequest request) {
        return userService.updateUserAsAdmin(id, request);
    }

    @PUT
    @Path("/me/notifications")
    @RolesAllowed({"DIVER", "DIVE_DIRECTOR", "ADMIN"})
    public UserResponse updateMyNotifPrefs(@Valid UpdateNotifPrefsRequest request) {
        return userService.updateNotifPrefs(identity.getPrincipal().getName(), request);
    }
}
