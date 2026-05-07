package org.santalina.diving.resource;

import org.santalina.diving.dto.AuthDto.*;
import org.santalina.diving.security.RateLimiter;
import org.santalina.diving.service.AuthService;
import org.santalina.diving.service.CaptchaService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.vertx.core.http.HttpServerRequest;
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
    RateLimiter rateLimiter;

    @Inject
    JsonWebToken jwt;

    @Context
    HttpServerRequest httpRequest;

    private String clientIp() {
        String forwarded = httpRequest.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return httpRequest.remoteAddress().host();
    }

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
        rateLimiter.check("register:" + clientIp(),
                RateLimiter.REGISTER_MAX_ATTEMPTS, RateLimiter.REGISTER_WINDOW_SECONDS);
        return Response.ok(authService.register(request)).build();
    }

    @POST
    @Path("/activate")
    @PermitAll
    public Response activateAccount(@Valid ActivateAccountRequest request) {
        return Response.ok(authService.activate(request)).build();
    }

    @POST
    @Path("/login")
    @PermitAll
    public Response login(@Valid LoginRequest request) {
        rateLimiter.check("login:" + clientIp(),
                RateLimiter.AUTH_MAX_ATTEMPTS, RateLimiter.AUTH_WINDOW_SECONDS);
        return Response.ok(authService.login(request)).build();
    }

    @POST
    @Path("/password-reset/request")
    @PermitAll
    public Response requestPasswordReset(@Valid PasswordResetRequest request) {
        rateLimiter.check("pwd-reset:" + clientIp(),
                RateLimiter.AUTH_MAX_ATTEMPTS, RateLimiter.AUTH_WINDOW_SECONDS);
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
