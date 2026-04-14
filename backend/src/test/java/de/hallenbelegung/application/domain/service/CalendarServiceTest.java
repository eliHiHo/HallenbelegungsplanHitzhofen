package de.hallenbelegung.application.domain.service;

import de.hallenbelegung.application.domain.exception.ForbiddenException;
import de.hallenbelegung.application.domain.exception.NotFoundException;
import de.hallenbelegung.application.domain.model.BlockedTime;
import de.hallenbelegung.application.domain.model.BlockedTimeType;
import de.hallenbelegung.application.domain.model.Booking;
import de.hallenbelegung.application.domain.model.BookingRequest;
import de.hallenbelegung.application.domain.model.BookingRequestStatus;
import de.hallenbelegung.application.domain.model.BookingSeriesRequest;
import de.hallenbelegung.application.domain.model.BookingStatus;
import de.hallenbelegung.application.domain.model.Hall;
import de.hallenbelegung.application.domain.model.HallType;
import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.out.BlockedTimeRepositoryPort;
import de.hallenbelegung.application.domain.port.out.BookingRepositoryPort;
import de.hallenbelegung.application.domain.port.out.BookingRequestRepositoryPort;
import de.hallenbelegung.application.domain.port.out.BookingSeriesRequestRepositoryPort;
import de.hallenbelegung.application.domain.port.out.UserRepositoryPort;
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
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CalendarServiceTest {

    private BookingRepositoryPort bookingRepository;
    private BlockedTimeRepositoryPort blockedTimeRepository;
    private BookingRequestRepositoryPort bookingRequestRepository;
    private UserRepositoryPort userRepository;
    private BookingSeriesRequestRepositoryPort bookingSeriesRequestRepository;

    private CalendarService service;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepositoryPort.class);
        blockedTimeRepository = mock(BlockedTimeRepositoryPort.class);
        bookingRequestRepository = mock(BookingRequestRepositoryPort.class);
        userRepository = mock(UserRepositoryPort.class);
        bookingSeriesRequestRepository = mock(BookingSeriesRequestRepositoryPort.class);

        service = new CalendarService(
                bookingRepository,
                blockedTimeRepository,
                bookingRequestRepository,
                userRepository,
                bookingSeriesRequestRepository
        );
    }

    private User user(Role role, boolean active, String firstName, String lastName) {
        return new User(
                UUID.randomUUID(),
                firstName,
                lastName,
                firstName.toLowerCase() + "@example.com",
                "hash",
                role,
                active,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z")
        );
    }

    private Hall hall(String name) {
        return new Hall(
                UUID.randomUUID(),
                name,
                "desc",
                true,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"),
                HallType.PART_SMALL
        );
    }

    private Booking booking(Hall hall, User owner, LocalDateTime start, LocalDateTime end, String title) {
        return new Booking(
                UUID.randomUUID(),
                title,
                "desc",
                start,
                end,
                BookingStatus.APPROVED,
                null,
                false,
                null,
                hall,
                owner,
                null,
                Instant.now(),
                Instant.now(),
                owner,
                null,
                null,
                null,
                null
        );
    }

    private BlockedTime blockedTime(Hall hall, User admin, LocalDateTime start, LocalDateTime end, String reason) {
        return new BlockedTime(
                UUID.randomUUID(),
                reason,
                start,
                end,
                BlockedTimeType.MANUAL,
                hall,
                admin,
                admin,
                Instant.now(),
                Instant.now()
        );
    }

    private BookingRequest pendingRequest(Hall hall, User requester, LocalDateTime start, LocalDateTime end, String title) {
        return new BookingRequest(
                UUID.randomUUID(),
                title,
                "desc",
                start,
                end,
                BookingRequestStatus.PENDING,
                null,
                hall,
                requester,
                null,
                Instant.now(),
                Instant.now(),
                null
        );
    }

    private BookingSeriesRequest pendingSeriesRequest(
            Hall hall,
            User requester,
            DayOfWeek weekday,
            LocalTime start,
            LocalTime end,
            LocalDate startDate,
            LocalDate endDate,
            String title
    ) {
        return new BookingSeriesRequest(
                UUID.randomUUID(),
                title,
                "desc",
                weekday,
                start,
                end,
                startDate,
                endDate,
                BookingRequestStatus.PENDING,
                null,
                hall,
                requester,
                null,
                Instant.now(),
                Instant.now(),
                null
        );
    }

    @Test
    void getWeek_returns_empty_for_week_with_no_events() {
        LocalDate weekStart = LocalDate.of(2026, 5, 4);
        LocalDateTime start = weekStart.atStartOfDay();
        LocalDateTime end = weekStart.plusDays(7).atStartOfDay();

        when(bookingRepository.findByTimeRange(start, end)).thenReturn(List.of());
        when(blockedTimeRepository.findAllByTimeRange(start, end)).thenReturn(List.of());

        CalendarWeekView result = service.getWeek(weekStart, null);

        assertTrue(result.entries().isEmpty());
    }

    @Test
    void getWeek_entry_starting_exactly_at_rangeEnd_is_excluded() {
        LocalDate weekStart = LocalDate.of(2026, 5, 4);
        LocalDateTime start = weekStart.atStartOfDay();
        LocalDateTime end = weekStart.plusDays(7).atStartOfDay();
        Hall hall = hall("Halle A");
        User owner = user(Role.CLUB_REPRESENTATIVE, true, "Owner", "Rep");

        when(bookingRepository.findByTimeRange(start, end)).thenReturn(List.of(
                booking(hall, owner, end, end.plusHours(1), "Outside")
        ));
        when(blockedTimeRepository.findAllByTimeRange(start, end)).thenReturn(List.of());

        CalendarWeekView result = service.getWeek(weekStart, null);

        assertTrue(result.entries().isEmpty());
    }

    @Test
    void getWeek_entry_ending_exactly_at_rangeStart_is_included() {
        LocalDate weekStart = LocalDate.of(2026, 5, 4);
        LocalDateTime start = weekStart.atStartOfDay();
        LocalDateTime end = weekStart.plusDays(7).atStartOfDay();
        Hall hall = hall("Halle A");
        User owner = user(Role.CLUB_REPRESENTATIVE, true, "Owner", "Rep");

        when(bookingRepository.findByTimeRange(start, end)).thenReturn(List.of(
                booking(hall, owner, start.minusHours(1), start.plusMinutes(1), "Spanning")
        ));
        when(blockedTimeRepository.findAllByTimeRange(start, end)).thenReturn(List.of());

        CalendarWeekView result = service.getWeek(weekStart, null);

        assertEquals(1, result.entries().size());
    }

    @Test
    void getDay_entry_completely_before_day_excluded() {
        LocalDate day = LocalDate.of(2026, 5, 4);
        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = day.plusDays(1).atStartOfDay();
        Hall hall = hall("Halle A");
        User owner = user(Role.CLUB_REPRESENTATIVE, true, "Owner", "Rep");

        when(bookingRepository.findByTimeRange(start, end)).thenReturn(List.of(
                booking(hall, owner, start.minusHours(2), start, "Before")
        ));
        when(blockedTimeRepository.findAllByTimeRange(start, end)).thenReturn(List.of());

        CalendarDayView result = service.getDay(day, null);

        assertTrue(result.entries().isEmpty());
    }

    @Test
    void getDay_cancelled_booking_is_still_included() {
        LocalDate day = LocalDate.of(2026, 5, 4);
        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = day.plusDays(1).atStartOfDay();
        Hall hall = hall("Halle A");
        User owner = user(Role.CLUB_REPRESENTATIVE, true, "Owner", "Rep");

        Booking approved = booking(hall, owner, LocalDateTime.of(2026, 5, 4, 10, 0), LocalDateTime.of(2026, 5, 4, 11, 0), "Approved");
        Booking cancelled = booking(hall, owner, LocalDateTime.of(2026, 5, 4, 12, 0), LocalDateTime.of(2026, 5, 4, 13, 0), "Cancelled");
        cancelled.cancel(owner, "reason");

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingRepository.findByTimeRange(start, end)).thenReturn(List.of(approved, cancelled));
        when(blockedTimeRepository.findAllByTimeRange(start, end)).thenReturn(List.of());
        when(bookingRequestRepository.findByStatus(BookingRequestStatus.PENDING)).thenReturn(List.of());
        when(bookingSeriesRequestRepository.findByStatus(BookingRequestStatus.PENDING)).thenReturn(List.of());

        CalendarDayView result = service.getDay(day, owner.getId());

        assertEquals(List.of("APPROVED", "CANCELLED"), result.entries().stream().map(CalendarEntryView::status).sorted().toList());
    }

    @Test
    void getDay_blocked_time_is_visible_for_guest() {
        LocalDate day = LocalDate.of(2026, 5, 4);
        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = day.plusDays(1).atStartOfDay();
        Hall hall = hall("Halle A");
        User admin = user(Role.ADMIN, true, "Admin", "One");

        when(bookingRepository.findByTimeRange(start, end)).thenReturn(List.of());
        when(blockedTimeRepository.findAllByTimeRange(start, end)).thenReturn(List.of(
                blockedTime(hall, admin, LocalDateTime.of(2026, 5, 4, 10, 0), LocalDateTime.of(2026, 5, 4, 11, 0), "Wartung")
        ));

        CalendarDayView result = service.getDay(day, null);

        assertEquals(CalendarEntryType.BLOCKED_TIME, result.entries().get(0).type());
        assertFalse(result.entries().get(0).ownEntry());
    }

    @Test
    void getDay_other_users_booking_not_marked_as_own_entry() {
        LocalDate day = LocalDate.of(2026, 5, 4);
        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = day.plusDays(1).atStartOfDay();
        Hall hall = hall("Halle A");
        User current = user(Role.CLUB_REPRESENTATIVE, true, "Current", "Rep");
        User other = user(Role.CLUB_REPRESENTATIVE, true, "Other", "Rep");

        when(userRepository.findById(current.getId())).thenReturn(Optional.of(current));
        when(bookingRepository.findByTimeRange(start, end)).thenReturn(List.of(
                booking(hall, other, LocalDateTime.of(2026, 5, 4, 14, 0), LocalDateTime.of(2026, 5, 4, 15, 0), "Foreign")
        ));
        when(blockedTimeRepository.findAllByTimeRange(start, end)).thenReturn(List.of());
        when(bookingRequestRepository.findByStatus(BookingRequestStatus.PENDING)).thenReturn(List.of());
        when(bookingSeriesRequestRepository.findByStatus(BookingRequestStatus.PENDING)).thenReturn(List.of());

        CalendarDayView result = service.getDay(day, current.getId());

        assertFalse(result.entries().get(0).ownEntry());
    }

    @Test
    void getDay_own_booking_marked_as_own_entry() {
        LocalDate day = LocalDate.of(2026, 5, 4);
        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = day.plusDays(1).atStartOfDay();
        Hall hall = hall("Halle A");
        User current = user(Role.CLUB_REPRESENTATIVE, true, "Current", "Rep");

        when(userRepository.findById(current.getId())).thenReturn(Optional.of(current));
        when(bookingRepository.findByTimeRange(start, end)).thenReturn(List.of(
                booking(hall, current, LocalDateTime.of(2026, 5, 4, 14, 0), LocalDateTime.of(2026, 5, 4, 15, 0), "Own")
        ));
        when(blockedTimeRepository.findAllByTimeRange(start, end)).thenReturn(List.of());
        when(bookingRequestRepository.findByStatus(BookingRequestStatus.PENDING)).thenReturn(List.of());
        when(bookingSeriesRequestRepository.findByStatus(BookingRequestStatus.PENDING)).thenReturn(List.of());

        CalendarDayView result = service.getDay(day, current.getId());

        assertTrue(result.entries().get(0).ownEntry());
    }

    @Test
    void getDay_blocked_time_never_marked_as_own() {
        LocalDate day = LocalDate.of(2026, 5, 4);
        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = day.plusDays(1).atStartOfDay();
        Hall hall = hall("Halle A");
        User current = user(Role.CLUB_REPRESENTATIVE, true, "Current", "Rep");
        User admin = user(Role.ADMIN, true, "Admin", "One");

        when(userRepository.findById(current.getId())).thenReturn(Optional.of(current));
        when(bookingRepository.findByTimeRange(start, end)).thenReturn(List.of());
        when(blockedTimeRepository.findAllByTimeRange(start, end)).thenReturn(List.of(
                blockedTime(hall, admin, LocalDateTime.of(2026, 5, 4, 10, 0), LocalDateTime.of(2026, 5, 4, 11, 0), "Blocked")
        ));
        when(bookingRequestRepository.findByStatus(BookingRequestStatus.PENDING)).thenReturn(List.of());
        when(bookingSeriesRequestRepository.findByStatus(BookingRequestStatus.PENDING)).thenReturn(List.of());

        CalendarDayView result = service.getDay(day, current.getId());

        assertFalse(result.entries().get(0).ownEntry());
    }

    @Test
    void getWeek_guest_does_not_see_pending_requests() {
        LocalDate weekStart = LocalDate.of(2026, 5, 4);
        LocalDateTime start = weekStart.atStartOfDay();
        LocalDateTime end = weekStart.plusDays(7).atStartOfDay();

        when(bookingRepository.findByTimeRange(start, end)).thenReturn(List.of());
        when(blockedTimeRepository.findAllByTimeRange(start, end)).thenReturn(List.of());

        service.getWeek(weekStart, null);

        verify(bookingRequestRepository, never()).findByStatus(BookingRequestStatus.PENDING);
    }

    @Test
    void getWeek_representative_sees_own_pending_series_request_occurrence() {
        LocalDate weekStart = LocalDate.of(2026, 5, 4);
        LocalDateTime start = weekStart.atStartOfDay();
        LocalDateTime end = weekStart.plusDays(7).atStartOfDay();
        Hall hall = hall("Halle A");
        User rep = user(Role.CLUB_REPRESENTATIVE, true, "Owner", "Rep");

        BookingSeriesRequest request = pendingSeriesRequest(
                hall,
                rep,
                DayOfWeek.MONDAY,
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                "Series"
        );

        when(userRepository.findById(rep.getId())).thenReturn(Optional.of(rep));
        when(bookingRepository.findByTimeRange(start, end)).thenReturn(List.of());
        when(blockedTimeRepository.findAllByTimeRange(start, end)).thenReturn(List.of());
        when(bookingRequestRepository.findByStatus(BookingRequestStatus.PENDING)).thenReturn(List.of());
        when(bookingSeriesRequestRepository.findByStatus(BookingRequestStatus.PENDING)).thenReturn(List.of(request));

        CalendarWeekView result = service.getWeek(weekStart, rep.getId());

        assertEquals(1, result.entries().stream().filter(e -> e.type() == CalendarEntryType.BOOKING_SERIES_REQUEST).count());
    }

    @Test
    void getWeek_representative_does_not_see_others_series_requests() {
        LocalDate weekStart = LocalDate.of(2026, 5, 4);
        LocalDateTime start = weekStart.atStartOfDay();
        LocalDateTime end = weekStart.plusDays(7).atStartOfDay();
        Hall hall = hall("Halle A");
        User rep = user(Role.CLUB_REPRESENTATIVE, true, "Owner", "Rep");
        User other = user(Role.CLUB_REPRESENTATIVE, true, "Other", "Rep");

        BookingSeriesRequest request = pendingSeriesRequest(
                hall,
                other,
                DayOfWeek.MONDAY,
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                "Series"
        );

        when(userRepository.findById(rep.getId())).thenReturn(Optional.of(rep));
        when(bookingRepository.findByTimeRange(start, end)).thenReturn(List.of());
        when(blockedTimeRepository.findAllByTimeRange(start, end)).thenReturn(List.of());
        when(bookingRequestRepository.findByStatus(BookingRequestStatus.PENDING)).thenReturn(List.of());
        when(bookingSeriesRequestRepository.findByStatus(BookingRequestStatus.PENDING)).thenReturn(List.of(request));

        CalendarWeekView result = service.getWeek(weekStart, rep.getId());

        assertTrue(result.entries().isEmpty());
    }

    @Test
    void getWeek_series_request_with_multiple_occurrences_in_week_creates_multiple_entries() {
        LocalDate weekStart = LocalDate.of(2026, 5, 4);
        LocalDateTime start = weekStart.atStartOfDay();
        LocalDateTime end = weekStart.plusDays(7).atStartOfDay();
        Hall hall = hall("Halle A");
        User rep = user(Role.CLUB_REPRESENTATIVE, true, "Owner", "Rep");

        BookingSeriesRequest request = pendingSeriesRequest(
                hall,
                rep,
                DayOfWeek.MONDAY,
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                "Series"
        );

        when(userRepository.findById(rep.getId())).thenReturn(Optional.of(rep));
        when(bookingRepository.findByTimeRange(start, end)).thenReturn(List.of());
        when(blockedTimeRepository.findAllByTimeRange(start, end)).thenReturn(List.of());
        when(bookingRequestRepository.findByStatus(BookingRequestStatus.PENDING)).thenReturn(List.of());
        when(bookingSeriesRequestRepository.findByStatus(BookingRequestStatus.PENDING)).thenReturn(List.of(request, request));

        CalendarWeekView result = service.getWeek(weekStart, rep.getId());

        assertEquals(2, result.entries().stream().filter(e -> e.type() == CalendarEntryType.BOOKING_SERIES_REQUEST).count());
    }

    @Test
    void getDay_series_request_occurrence_outside_series_date_range_excluded() {
        LocalDate day = LocalDate.of(2026, 5, 4);
        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = day.plusDays(1).atStartOfDay();
        Hall hall = hall("Halle A");
        User rep = user(Role.CLUB_REPRESENTATIVE, true, "Owner", "Rep");

        BookingSeriesRequest request = pendingSeriesRequest(
                hall,
                rep,
                DayOfWeek.MONDAY,
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                day.plusDays(1),
                day.plusDays(30),
                "Series"
        );

        when(userRepository.findById(rep.getId())).thenReturn(Optional.of(rep));
        when(bookingRepository.findByTimeRange(start, end)).thenReturn(List.of());
        when(blockedTimeRepository.findAllByTimeRange(start, end)).thenReturn(List.of());
        when(bookingRequestRepository.findByStatus(BookingRequestStatus.PENDING)).thenReturn(List.of());
        when(bookingSeriesRequestRepository.findByStatus(BookingRequestStatus.PENDING)).thenReturn(List.of(request));

        CalendarDayView result = service.getDay(day, rep.getId());

        assertTrue(result.entries().isEmpty());
    }

    @Test
    void getDay_entries_sorted_ascending_across_types() {
        LocalDate day = LocalDate.of(2026, 5, 4);
        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = day.plusDays(1).atStartOfDay();
        Hall hall = hall("Halle A");
        User rep = user(Role.CLUB_REPRESENTATIVE, true, "Owner", "Rep");
        User admin = user(Role.ADMIN, true, "Admin", "One");

        Booking booking = booking(hall, rep, LocalDateTime.of(2026, 5, 4, 14, 0), LocalDateTime.of(2026, 5, 4, 15, 0), "Booking");
        BlockedTime blocked = blockedTime(hall, admin, LocalDateTime.of(2026, 5, 4, 10, 0), LocalDateTime.of(2026, 5, 4, 11, 0), "Blocked");
        BookingRequest request = pendingRequest(hall, rep, LocalDateTime.of(2026, 5, 4, 12, 0), LocalDateTime.of(2026, 5, 4, 13, 0), "Request");

        when(userRepository.findById(rep.getId())).thenReturn(Optional.of(rep));
        when(bookingRepository.findByTimeRange(start, end)).thenReturn(List.of(booking));
        when(blockedTimeRepository.findAllByTimeRange(start, end)).thenReturn(List.of(blocked));
        when(bookingRequestRepository.findByStatus(BookingRequestStatus.PENDING)).thenReturn(List.of(request));
        when(bookingSeriesRequestRepository.findByStatus(BookingRequestStatus.PENDING)).thenReturn(List.of());

        CalendarDayView result = service.getDay(day, rep.getId());

        assertEquals(
                List.of(
                        LocalDateTime.of(2026, 5, 4, 10, 0),
                        LocalDateTime.of(2026, 5, 4, 12, 0),
                        LocalDateTime.of(2026, 5, 4, 14, 0)
                ),
                result.entries().stream().map(CalendarEntryView::startDateTime).toList()
        );
    }

    @Test
    void getWeek_rejects_unknown_user() {
        UUID unknownUserId = UUID.randomUUID();

        when(userRepository.findById(unknownUserId)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> service.getWeek(LocalDate.of(2026, 5, 4), unknownUserId)
        );

        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void getWeek_rejects_inactive_user() {
        User inactive = user(Role.CLUB_REPRESENTATIVE, false, "Inactive", "Rep");

        when(userRepository.findById(inactive.getId())).thenReturn(Optional.of(inactive));

        ForbiddenException ex = assertThrows(
                ForbiddenException.class,
                () -> service.getWeek(LocalDate.of(2026, 5, 4), inactive.getId())
        );

        assertEquals("User inactive", ex.getMessage());
    }
}
