package de.hallenbelegung.adapters.in.api.rest;

import de.hallenbelegung.adapters.in.api.dto.BookingSeriesDTO;
import de.hallenbelegung.adapters.in.api.dto.EmptyResponseDTO;
import de.hallenbelegung.application.domain.exception.UnauthorizedException;
import de.hallenbelegung.application.domain.model.BookingSeries;
import de.hallenbelegung.application.domain.model.BookingSeriesStatus;
import de.hallenbelegung.application.domain.model.Hall;
import de.hallenbelegung.application.domain.model.HallType;
import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.in.CancelBookingSeriesOccurrenceUseCase;
import de.hallenbelegung.application.domain.port.in.CancelBookingSeriesUseCase;
import de.hallenbelegung.application.domain.port.in.GetBookingSeriesUseCase;
import de.hallenbelegung.application.domain.port.in.GetCurrentUserUseCase;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingSeriesResourceTest {

    private GetCurrentUserUseCase getCurrentUserUseCase;
    private GetBookingSeriesUseCase getBookingSeriesUseCase;
    private CancelBookingSeriesUseCase cancelBookingSeriesUseCase;
    private CancelBookingSeriesOccurrenceUseCase cancelBookingSeriesOccurrenceUseCase;

    private BookingSeriesResource resource;

    @BeforeEach
    void setUp() {
        getCurrentUserUseCase = mock(GetCurrentUserUseCase.class);
        getBookingSeriesUseCase = mock(GetBookingSeriesUseCase.class);
        cancelBookingSeriesUseCase = mock(CancelBookingSeriesUseCase.class);
        cancelBookingSeriesOccurrenceUseCase = mock(CancelBookingSeriesOccurrenceUseCase.class);

        resource = new BookingSeriesResource(
                getCurrentUserUseCase,
                getBookingSeriesUseCase,
                cancelBookingSeriesUseCase,
                cancelBookingSeriesOccurrenceUseCase
        );
    }

    private User user(UUID id) {
        return new User(id, "Max", "Mustermann", "max@example.com", "hash", Role.ADMIN, true, Instant.now(), Instant.now());
    }

    private Hall hall() {
        return new Hall(UUID.randomUUID(), "Halle A", "desc", true, Instant.now(), Instant.now(), HallType.PART_SMALL);
    }

    private BookingSeries series(UUID id, User owner, Hall hall) {
        return new BookingSeries(
                id,
                "Serie",
                "desc",
                DayOfWeek.MONDAY,
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 1),
                BookingSeriesStatus.ACTIVE,
                hall,
                owner,
                owner,
                owner,
                null,
                Instant.now(),
                Instant.now(),
                null,
                null
        );
    }

    @Test
    void getById_requires_cookie_and_maps_series() {
        UUID seriesId = UUID.randomUUID();

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> resource.getById(seriesId, " ")
        );
        assertEquals("Missing session cookie", ex.getMessage());

        UUID userId = UUID.randomUUID();
        User current = user(userId);
        Hall hall = hall();
        BookingSeries found = series(seriesId, current, hall);

        when(getCurrentUserUseCase.getCurrentUser("sess")).thenReturn(current);
        when(getBookingSeriesUseCase.getById(userId, seriesId)).thenReturn(found);

        BookingSeriesDTO dto = resource.getById(seriesId, "sess");

        assertEquals(seriesId, dto.id());
        assertEquals("Serie", dto.title());
        assertEquals(hall.getId(), dto.hallId());
    }

    @Test
    void cancelSeries_passes_reason_and_returns_empty_response() {
        UUID seriesId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User current = user(userId);
        when(getCurrentUserUseCase.getCurrentUser("sess")).thenReturn(current);

        Response response = resource.cancelSeries(seriesId, "Wird nicht mehr benoetigt", "sess");

        assertEquals(200, response.getStatus());
        assertInstanceOf(EmptyResponseDTO.class, response.getEntity());
        verify(cancelBookingSeriesUseCase).cancelSeries(userId, seriesId, "Wird nicht mehr benoetigt");
    }

    @Test
    void cancelOccurrence_passes_ids_and_reason() {
        UUID seriesId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User current = user(userId);
        when(getCurrentUserUseCase.getCurrentUser("sess")).thenReturn(current);

        Response response = resource.cancelOccurrence(seriesId, bookingId, "Einzeltermin faellt aus", "sess");

        assertEquals(200, response.getStatus());
        assertInstanceOf(EmptyResponseDTO.class, response.getEntity());
        verify(cancelBookingSeriesOccurrenceUseCase).cancelSingleOccurrence(userId, seriesId, bookingId, "Einzeltermin faellt aus");
    }
}

