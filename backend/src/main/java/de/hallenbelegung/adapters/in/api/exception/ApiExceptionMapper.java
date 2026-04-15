package de.hallenbelegung.adapters.in.api.exception;

import de.hallenbelegung.adapters.in.api.dto.ErrorResponseDTO;
import de.hallenbelegung.application.domain.exception.BookingConflictException;
import de.hallenbelegung.application.domain.exception.DomainException;
import de.hallenbelegung.application.domain.exception.ForbiddenException;
import de.hallenbelegung.application.domain.exception.NotFoundException;
import de.hallenbelegung.application.domain.exception.RateLimitException;
import de.hallenbelegung.application.domain.exception.UnauthorizedException;
import de.hallenbelegung.application.domain.exception.ValidationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.core.Context;

import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(ApiExceptionMapper.class.getName());

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof DomainException domainEx) {
            return handleDomainException(domainEx);
        }

        // Unexpected technical error -> 500
        LOG.log(Level.SEVERE, "Unexpected error handling request", exception);
        ErrorResponseDTO body = new ErrorResponseDTO(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "INTERNAL_SERVER_ERROR",
                exception.getMessage() != null ? exception.getMessage() : "Unexpected error",
                Instant.now(),
                uriInfo != null && uriInfo.getRequestUri() != null ? uriInfo.getRequestUri().getPath() : null
        );

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }

    private Response handleDomainException(DomainException ex) {
        int status;
        String error;

        if (ex instanceof NotFoundException) {
            status = Response.Status.NOT_FOUND.getStatusCode();
            error = "NOT_FOUND";
        } else if (ex instanceof ForbiddenException) {
            status = Response.Status.FORBIDDEN.getStatusCode();
            error = "FORBIDDEN";
        } else if (ex instanceof UnauthorizedException) {
            status = Response.Status.UNAUTHORIZED.getStatusCode();
            error = "UNAUTHORIZED";
        } else if (ex instanceof ValidationException) {
            status = Response.Status.BAD_REQUEST.getStatusCode();
            error = "VALIDATION_ERROR";
        } else if (ex instanceof BookingConflictException) {
            status = Response.Status.CONFLICT.getStatusCode();
            error = "CONFLICT";
        } else if (ex instanceof RateLimitException) {
            status = Response.Status.TOO_MANY_REQUESTS.getStatusCode();
            error = "RATE_LIMIT_EXCEEDED";
        } else {
            // Generic domain exception -> 400 Bad Request (or 422?)
            status = Response.Status.BAD_REQUEST.getStatusCode();
            error = ex.getErrorCode() != null ? ex.getErrorCode() : "DOMAIN_ERROR";
        }

        ErrorResponseDTO body = new ErrorResponseDTO(
                status,
                error,
                ex.getMessage(),
                Instant.now(),
                uriInfo != null && uriInfo.getRequestUri() != null ? uriInfo.getRequestUri().getPath() : null
        );

        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
