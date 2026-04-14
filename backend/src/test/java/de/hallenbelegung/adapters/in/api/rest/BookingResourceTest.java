package de.hallenbelegung.adapters.in.api.rest;

import de.hallenbelegung.adapters.in.api.dto.BookingDTO;
import de.hallenbelegung.adapters.in.api.dto.BookingFeedbackDTO;
import de.hallenbelegung.adapters.in.api.dto.BookingRequestDTO;
import de.hallenbelegung.adapters.in.api.dto.EmptyResponseDTO;
import de.hallenbelegung.application.domain.exception.UnauthorizedException;
import de.hallenbelegung.application.domain.exception.ValidationException;
import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.in.CancelBookingUseCase;
import de.hallenbelegung.application.domain.port.in.GetBookingUseCase;
import de.hallenbelegung.application.domain.port.in.GetCurrentUserUseCase;
import de.hallenbelegung.application.domain.port.in.UpdateBookingFeedbackUseCase;
import de.hallenbelegung.application.domain.port.in.UpdateBookingUseCase;
import de.hallenbelegung.application.domain.view.BookingDetailView;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingResourceTest {

    private GetCurrentUserUseCase getCurrentUserUseCase;
    private GetBookingUseCase getBookingUseCase;
    private UpdateBookingUseCase updateBookingUseCase;
    private CancelBookingUseCase cancelBookingUseCase;
    private UpdateBookingFeedbackUseCase updateBookingFeedbackUseCase;

    private BookingResource resource;

    @BeforeEach
    void setUp() {
        getCurrentUserUseCase = mock(GetCurrentUserUseCase.class);
        getBookingUseCase = mock(GetBookingUseCase.class);
        updateBookingUseCase = mock(UpdateBookingUseCase.class);
        cancelBookingUseCase = mock(CancelBookingUseCase.class);
        updateBookingFeedbackUseCase = mock(UpdateBookingFeedbackUseCase.class);

        resource = new BookingResource(
                getCurrentUserUseCase,
                getBookingUseCase,
                updateBookingUseCase,
                cancelBookingUseCase,
                updateBookingFeedbackUseCase
        );
    }

    private User user(UUID id) {
        return new User(id, "Max", "Mustermann", "max@example.com", "hash", Role.ADMIN, true, Instant.now(), Instant.now());
    }

    private BookingDetailView detail(UUID bookingId) {
        return new BookingDetailView(
                bookingId,
                "Training",
                "desc",
                LocalDateTime.of(2026, 5, 4, 10, 0),
                LocalDateTime.of(2026, 5, 4, 11, 0),
                UUID.randomUUID(),
                "Halle A",
                "APPROVED",
                "Max Mustermann",
                10,
                "gut",
                true,
                true,
                true
        );
    }

    @Test
    void getById_allows_anonymous_when_cookie_missing() {
        UUID bookingId = UUID.randomUUID();
        when(getBookingUseCase.getById(null, bookingId)).thenReturn(detail(bookingId));

        BookingDTO dto = resource.getById(bookingId, null);

        assertEquals(bookingId, dto.id());
        verify(getCurrentUserUseCase, never()).getCurrentUser("anything");
    }

    @Test
    void getById_with_cookie_resolves_current_user() {
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(getCurrentUserUseCase.getCurrentUser("sess")).thenReturn(user(userId));
        when(getBookingUseCase.getById(userId, bookingId)).thenReturn(detail(bookingId));

        BookingDTO dto = resource.getById(bookingId, "sess");

        assertEquals("Training", dto.title());
        verify(getBookingUseCase).getById(userId, bookingId);
    }

    @Test
    void update_validates_required_fields() {
        UUID bookingId = UUID.randomUUID();
        when(getCurrentUserUseCase.getCurrentUser("sess")).thenReturn(user(UUID.randomUUID()));

        ValidationException ex1 = assertThrows(ValidationException.class, () -> resource.update(bookingId, null, "sess"));
        assertEquals("Request body is required", ex1.getMessage());

        BookingRequestDTO missingHall = new BookingRequestDTO(
                null,
                "T",
                null,
                LocalDateTime.of(2026, 5, 4, 10, 0),
                LocalDateTime.of(2026, 5, 4, 11, 0),
                null,
                null,
                null,
                null,
                null
        );
        ValidationException ex2 = assertThrows(ValidationException.class, () -> resource.update(bookingId, missingHall, "sess"));
        assertEquals("hallId is required", ex2.getMessage());
    }

    @Test
    void update_calls_use_case_and_returns_refetched_booking() {
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID hallId = UUID.randomUUID();
        User current = user(userId);

        BookingRequestDTO request = new BookingRequestDTO(
                null,
                "Neu",
                "desc",
                LocalDateTime.of(2026, 5, 4, 12, 0),
                LocalDateTime.of(2026, 5, 4, 13, 0),
                hallId,
                null,
                null,
                null,
                null
        );

        when(getCurrentUserUseCase.getCurrentUser("sess")).thenReturn(current);
        when(getBookingUseCase.getById(userId, bookingId)).thenReturn(detail(bookingId));

        BookingDTO dto = resource.update(bookingId, request, "sess");

        assertEquals(bookingId, dto.id());
        verify(updateBookingUseCase).update(
                userId,
                bookingId,
                hallId,
                "Neu",
                "desc",
                request.startDateTime(),
                request.endDateTime()
        );
        verify(getBookingUseCase).getById(userId, bookingId);
    }

    @Test
    void cancel_requires_cookie_and_passes_reason() {
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> resource.cancel(bookingId, "Grund", "   ")
        );
        assertEquals("Missing session cookie", ex.getMessage());

        when(getCurrentUserUseCase.getCurrentUser("sess")).thenReturn(user(userId));

        Response response = resource.cancel(bookingId, "Grund", "sess");

        assertEquals(200, response.getStatus());
        assertInstanceOf(EmptyResponseDTO.class, response.getEntity());
        verify(cancelBookingUseCase).cancel(userId, bookingId, "Grund");
    }

    @Test
    void updateFeedback_requires_body_and_cookie_then_calls_use_case() {
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> resource.updateFeedback(bookingId, new BookingFeedbackDTO(5, "ok"), null)
        );
        assertEquals("Missing session cookie", ex.getMessage());

        when(getCurrentUserUseCase.getCurrentUser("sess")).thenReturn(user(userId));

        ValidationException ex2 = assertThrows(
                ValidationException.class,
                () -> resource.updateFeedback(bookingId, null, "sess")
        );
        assertEquals("Request body is required", ex2.getMessage());

        Response response = resource.updateFeedback(bookingId, new BookingFeedbackDTO(5, "ok"), "sess");

        assertEquals(200, response.getStatus());
        assertInstanceOf(EmptyResponseDTO.class, response.getEntity());
        verify(updateBookingFeedbackUseCase).updateFeedback(bookingId, 5, "ok", userId);
    }
}

