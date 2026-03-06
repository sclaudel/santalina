package com.example.diving.exception;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;
import java.util.stream.Collectors;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof NotFoundException e) {
            return error(404, e.getMessage());
        }
        if (exception instanceof BadRequestException e) {
            return error(400, e.getMessage());
        }
        if (exception instanceof NotAuthorizedException e) {
            return error(401, "Non autorisé");
        }
        if (exception instanceof ForbiddenException e) {
            return error(403, e.getMessage());
        }
        if (exception instanceof ConstraintViolationException e) {
            String msg = e.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));
            return error(400, msg);
        }
        return error(500, "Erreur interne du serveur");
    }

    private Response error(int status, String message) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("status", status, "message", message != null ? message : "Erreur"))
                .build();
    }
}

