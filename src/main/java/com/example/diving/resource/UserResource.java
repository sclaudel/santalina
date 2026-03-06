package com.example.diving.resource;

import com.example.diving.dto.UserDto.*;
import com.example.diving.service.UserService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
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

    @GET
    @Path("/me")
    public UserResponse getProfile() {
        return userService.getProfile(jwt.getName());
    }

    @PUT
    @Path("/me")
    public UserResponse updateProfile(@Valid UpdateProfileRequest request) {
        return userService.updateProfile(jwt.getName(), request);
    }

    @GET
    @RolesAllowed("ADMIN")
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @PUT
    @Path("/{id}/role")
    @RolesAllowed("ADMIN")
    public UserResponse updateRole(@PathParam("id") Long id, @Valid UpdateRoleRequest request) {
        return userService.updateRole(id, request);
    }
}

