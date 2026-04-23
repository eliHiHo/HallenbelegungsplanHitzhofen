package de.hallenbelegung.adapters.in.api.rest;

import de.hallenbelegung.adapters.in.api.dto.CalendarDayDTO;
import de.hallenbelegung.adapters.in.api.dto.CalendarWeekDTO;
import de.hallenbelegung.application.domain.exception.UnauthorizedException;
import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.in.GetCalendarDayUseCase;
import de.hallenbelegung.application.domain.port.in.GetCalendarWeekUseCase;
import de.hallenbelegung.application.domain.port.in.GetCurrentUserUseCase;
import de.hallenbelegung.application.domain.view.CalendarDayView;
import de.hallenbelegung.application.domain.view.CalendarEntryType;
import de.hallenbelegung.application.domain.view.CalendarEntryView;
import de.hallenbelegung.application.domain.view.CalendarWeekView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CalendarResourceTest {

    private GetCalendarWeekUseCase getCalendarWeekUseCase;
    private GetCalendarDayUseCase getCalendarDayUseCase;
    private GetCurrentUserUseCase getCurrentUserUseCase;

    private CalendarResource resource;

    @BeforeEach
    void setUp() {
        getCalendarWeekUseCase = mock(GetCalendarWeekUseCase.class);
        getCalendarDayUseCase = mock(GetCalendarDayUseCase.class);
        getCurrentUserUseCase = mock(GetCurrentUserUseCase.class);

        resource = new CalendarResource(getCalendarWeekUseCase, getCalendarDayUseCase, getCurrentUserUseCase);
    }

    private User user(UUID id) {
        return new User(id, "Max", "Mustermann", "max@example.com", "hash", Role.CLUB_REPRESENTATIVE, true, Instant.now(), Instant.now());
    }

    @Test
    void getWeek_normalizes_to_monday_and_maps_result() {
        LocalDate input = LocalDate.of(2026, 5, 6); // Wednesday
        LocalDate monday = LocalDate.of(2026, 5, 4);

        CalendarWeekView view = new CalendarWeekView(
                monday,
                monday.plusDays(6),
                List.of(new CalendarEntryView(
                        UUID.randomUUID(),
                        CalendarEntryType.BOOKING,
                        "Training",
                        "desc",
                        LocalDateTime.of(2026, 5, 4, 10, 0),
                        LocalDateTime.of(2026, 5, 4, 11, 0),
                        UUID.randomUUID(),
                        "Halle A",
                        "Max Mustermann",
                        "APPROVED",
                        true,
                        null
                ))
        );

        when(getCalendarWeekUseCase.getWeek(monday, null)).thenReturn(view);

        CalendarWeekDTO dto = resource.getWeek(input, null);

        assertEquals(monday, dto.weekStart());
        assertEquals(1, dto.entries().size());
        assertEquals("BOOKING", dto.entries().get(0).type());
        verify(getCalendarWeekUseCase).getWeek(monday, null);
    }

    @Test
    void getWeek_with_valid_cookie_resolves_user() {
        UUID userId = UUID.randomUUID();
        LocalDate monday = LocalDate.of(2026, 5, 4);
        when(getCurrentUserUseCase.getCurrentUser("sess")).thenReturn(user(userId));
        when(getCalendarWeekUseCase.getWeek(monday, userId)).thenReturn(new CalendarWeekView(monday, monday.plusDays(6), List.of()));

        resource.getWeek(monday, "sess");

        verify(getCalendarWeekUseCase).getWeek(monday, userId);
    }

    @Test
    void getWeek_with_invalid_cookie_falls_back_to_anonymous() {
        LocalDate monday = LocalDate.of(2026, 5, 4);
        when(getCurrentUserUseCase.getCurrentUser("bad")).thenThrow(new UnauthorizedException("invalid"));
        when(getCalendarWeekUseCase.getWeek(monday, null)).thenReturn(new CalendarWeekView(monday, monday.plusDays(6), List.of()));

        resource.getWeek(monday, "bad");

        verify(getCalendarWeekUseCase).getWeek(monday, null);
    }

    @Test
    void getDay_with_blank_cookie_is_anonymous_and_maps() {
        LocalDate day = LocalDate.of(2026, 5, 4);
        CalendarDayView view = new CalendarDayView(day, List.of());
        when(getCalendarDayUseCase.getDay(day, null)).thenReturn(view);

        CalendarDayDTO dto = resource.getDay(day, "   ");

        assertEquals(day, dto.day());
        verify(getCalendarDayUseCase).getDay(day, null);
    }
}

