package org.santalina.diving.exception;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.stream.Collectors;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof NotFoundException e) {
            LOG.debugf("404 Not Found: %s", e.getMessage());
            return error(404, e.getMessage());
        }
        if (exception instanceof BadRequestException e) {
            LOG.debugf("400 Bad Request: %s", e.getMessage());
            return error(400, e.getMessage());
        }
        if (exception instanceof NotAuthorizedException e) {
            LOG.debugf("401 Unauthorized: %s", e.getMessage());
            return error(401, "Non autorisé");
        }
        if (exception instanceof ForbiddenException e) {
            LOG.warnf("403 Forbidden: %s", e.getMessage());
            return error(403, e.getMessage());
        }
        if (exception instanceof ServiceUnavailableException e) {
            LOG.warnf("503 Service Unavailable: %s", e.getMessage());
            return error(503, e.getMessage());
        }
        if (exception instanceof ConstraintViolationException e) {
            String msg = e.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));
            LOG.debugf("400 Constraint Violation: %s", msg);
            return error(400, msg);
        }
        LOG.errorf(exception, "500 Unhandled exception: %s", exception.getMessage());
        return error(500, "Erreur interne du serveur");
    }

    private Response error(int status, String message) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("status", status, "message", message != null ? message : "Erreur"))
                .build();
    }
}
