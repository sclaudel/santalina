package com.example.diving.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Surcharge le handler Quarkus par défaut pour les violations de contrainte.
 * Retourne des messages explicites au lieu du titre générique "Constraint Violation".
 */
@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));

        if (message.isBlank()) {
            message = "Erreur de validation";
        }

        return Response.status(400)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("status", 400, "message", message))
                .build();
    }
}
