package org.santalina.diving.resource;

import org.santalina.diving.dto.AuthDto.*;
import org.santalina.diving.service.AuthService;
import org.santalina.diving.service.CaptchaService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Authentification")
public class AuthResource {

    @Inject
    AuthService authService;

    @Inject
    CaptchaService captchaService;

    @Inject
    JsonWebToken jwt;

    @GET
    @Path("/captcha")
    @PermitAll
    public CaptchaService.CaptchaChallenge getCaptcha() {
        return captchaService.generate();
    }

    @POST
    @Path("/register")
    @PermitAll
    public Response register(@Valid RegisterRequest request) {
        return Response.status(201).entity(authService.register(request)).build();
    }

    @POST
    @Path("/login")
    @PermitAll
    public Response login(@Valid LoginRequest request) {
        return Response.ok(authService.login(request)).build();
    }

    @POST
    @Path("/password-reset/request")
    @PermitAll
    public Response requestPasswordReset(@Valid PasswordResetRequest request) {
        authService.requestPasswordReset(request);
        return Response.ok(java.util.Map.of("message",
                "Si cet email existe, un lien de réinitialisation a été envoyé")).build();
    }

    @POST
    @Path("/password-reset/confirm")
    @PermitAll
    public Response confirmPasswordReset(@Valid PasswordResetConfirm request) {
        authService.confirmPasswordReset(request);
        return Response.ok(java.util.Map.of("message", "Mot de passe modifié avec succès")).build();
    }

    @POST
    @Path("/change-password")
    @RolesAllowed({"ADMIN", "DIVE_DIRECTOR", "DIVER"})
    public Response changePassword(@Valid ChangePasswordRequest request) {
        authService.changePassword(jwt.getName(), request);
        return Response.ok(java.util.Map.of("message", "Mot de passe modifié avec succès")).build();
    }
}
