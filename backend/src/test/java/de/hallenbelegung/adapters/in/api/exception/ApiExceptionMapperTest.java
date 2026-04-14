package de.hallenbelegung.adapters.in.api.exception;

import de.hallenbelegung.adapters.in.api.dto.ErrorResponseDTO;
import de.hallenbelegung.application.domain.exception.BookingConflictException;
import de.hallenbelegung.application.domain.exception.DomainException;
import de.hallenbelegung.application.domain.exception.ForbiddenException;
import de.hallenbelegung.application.domain.exception.NotFoundException;
import de.hallenbelegung.application.domain.exception.RateLimitException;
import de.hallenbelegung.application.domain.exception.UnauthorizedException;
import de.hallenbelegung.application.domain.exception.ValidationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiExceptionMapperTest {

    private ApiExceptionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ApiExceptionMapper();
        mapper.uriInfo = null;
    }

    @Test
    void toResponse_maps_not_found_exception_to_404() {
        Response response = mapper.toResponse(new NotFoundException("not found"));

        ErrorResponseDTO body = assertDomainErrorBody(response, 404, "NOT_FOUND", "not found");
        assertNull(body.path());
    }

    @Test
    void toResponse_maps_forbidden_exception_to_403() {
        Response response = mapper.toResponse(new ForbiddenException("forbidden"));

        ErrorResponseDTO body = assertDomainErrorBody(response, 403, "FORBIDDEN", "forbidden");
        assertNull(body.path());
    }

    @Test
    void toResponse_maps_unauthorized_exception_to_401() {
        Response response = mapper.toResponse(new UnauthorizedException("unauthorized"));

        ErrorResponseDTO body = assertDomainErrorBody(response, 401, "UNAUTHORIZED", "unauthorized");
        assertNull(body.path());
    }

    @Test
    void toResponse_maps_validation_exception_to_400() {
        Response response = mapper.toResponse(new ValidationException("validation failed"));

        ErrorResponseDTO body = assertDomainErrorBody(response, 400, "VALIDATION_ERROR", "validation failed");
        assertNull(body.path());
    }

    @Test
    void toResponse_maps_booking_conflict_exception_to_409() {
        Response response = mapper.toResponse(new BookingConflictException("conflict"));

        ErrorResponseDTO body = assertDomainErrorBody(response, 409, "CONFLICT", "conflict");
        assertNull(body.path());
    }

    @Test
    void toResponse_maps_rate_limit_exception_to_429() {
        Response response = mapper.toResponse(new RateLimitException("too many requests"));

        ErrorResponseDTO body = assertDomainErrorBody(response, 429, "RATE_LIMIT_EXCEEDED", "too many requests");
        assertNull(body.path());
    }

    @Test
    void toResponse_maps_generic_domain_exception_with_custom_error_code_to_400() {
        Response response = mapper.toResponse(new TestDomainException("CUSTOM_ERROR", "domain problem"));

        ErrorResponseDTO body = assertDomainErrorBody(response, 400, "CUSTOM_ERROR", "domain problem");
        assertNull(body.path());
    }

    @Test
    void toResponse_maps_generic_domain_exception_without_error_code_to_400_domain_error() {
        Response response = mapper.toResponse(new TestDomainException(null, "domain problem"));

        ErrorResponseDTO body = assertDomainErrorBody(response, 400, "DOMAIN_ERROR", "domain problem");
        assertNull(body.path());
    }

    @Test
    void toResponse_maps_runtime_exception_to_500() {
        Response response = mapper.toResponse(new RuntimeException("boom"));

        ErrorResponseDTO body = assertDomainErrorBody(response, 500, "INTERNAL_SERVER_ERROR", "boom");
        assertNull(body.path());
    }

    @Test
    void toResponse_maps_runtime_exception_with_null_message_to_unexpected_error() {
        Response response = mapper.toResponse(new RuntimeException((String) null));

        ErrorResponseDTO body = assertDomainErrorBody(response, 500, "INTERNAL_SERVER_ERROR", "Unexpected error");
        assertNull(body.path());
    }

    private ErrorResponseDTO assertDomainErrorBody(Response response, int status, String error, String message) {
        assertEquals(status, response.getStatus());
        Object entity = response.getEntity();
        ErrorResponseDTO body = assertInstanceOf(ErrorResponseDTO.class, entity);
        assertEquals(status, body.status());
        assertEquals(error, body.error());
        assertEquals(message, body.message());
        assertNotNull(body.timestamp());
        return body;
    }

    private static final class TestDomainException extends DomainException {
        private TestDomainException(String errorCode, String message) {
            super(errorCode, message);
        }
    }
}

