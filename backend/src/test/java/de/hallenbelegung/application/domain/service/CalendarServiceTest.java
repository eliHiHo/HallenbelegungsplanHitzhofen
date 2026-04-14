package de.hallenbelegung.application.domain.service;

import de.hallenbelegung.application.domain.exception.ForbiddenException;
import de.hallenbelegung.application.domain.exception.NotFoundException;
import de.hallenbelegung.application.domain.model.BlockedTime;
import de.hallenbelegung.application.domain.model.BlockedTimeType;
import de.hallenbelegung.application.domain.model.Booking;
import de.hallenbelegung.application.domain.model.BookingRequest;
import de.hallenbelegung.application.domain.model.BookingRequestStatus;
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

    private Hall hall(HallType hallType, String name) {
        return new Hall(
                UUID.randomUUID(),
                name,
                "desc",
                true,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"),
                hallType
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

    @Test
    void getWeek_guest_sees_bookings_and_blocked_times_sorted_without_requests() {
        LocalDate weekStart = LocalDate.of(2026, 5, 4);
        LocalDateTime rangeStart = weekStart.atStartOfDay();
        LocalDateTime rangeEnd = weekStart.plusDays(7).atStartOfDay();

        User representative = user(Role.CLUB_REPRESENTATIVE, true, "Rep", "One");
        User admin = user(Role.ADMIN, true, "Admin", "One");
        Hall hall = hall(HallType.PART_SMALL, "Halle A");

        Booking insideBooking = booking(hall, representative,
                LocalDateTime.of(2026, 5, 5, 10, 0),
                LocalDateTime.of(2026, 5, 5, 11, 0),
                "Training");
        Booking outsideEdgeBooking = booking(hall, representative,
                LocalDateTime.of(2026, 5, 11, 0, 0),
                LocalDateTime.of(2026, 5, 11, 1, 0),
                "Outside");

        BlockedTime blocked = blockedTime(hall, admin,
                LocalDateTime.of(2026, 5, 4, 8, 0),
                LocalDateTime.of(2026, 5, 4, 9, 0),
                "Wartung");

        when(bookingRepository.findByTimeRange(rangeStart, rangeEnd)).thenReturn(List.of(insideBooking, outsideEdgeBooking));
        when(blockedTimeRepository.findAllByTimeRange(rangeStart, rangeEnd)).thenReturn(List.of(blocked));

        CalendarWeekView result = service.getWeek(weekStart, null);

        assertEquals(weekStart, result.weekStart());
        assertEquals(weekStart.plusDays(6), result.weekEnd());
        assertEquals(2, result.entries().size());
        assertEquals(CalendarEntryType.BLOCKED_TIME, result.entries().get(0).type());
        assertEquals(CalendarEntryType.BOOKING, result.entries().get(1).type());
        assertFalse(result.entries().get(1).ownEntry());

        verify(bookingRequestRepository, never()).findByStatus(BookingRequestStatus.PENDING);
        verify(bookingSeriesRequestRepository, never()).findByStatus(BookingRequestStatus.PENDING);
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
        User inactiveUser = user(Role.CLUB_REPRESENTATIVE, false, "Inactive", "Rep");

        when(userRepository.findById(inactiveUser.getId())).thenReturn(Optional.of(inactiveUser));

        ForbiddenException ex = assertThrows(
                ForbiddenException.class,
                () -> service.getWeek(LocalDate.of(2026, 5, 4), inactiveUser.getId())
        );

        assertEquals("User inactive", ex.getMessage());
    }

    @Test
    void getDay_admin_sees_pending_requests_and_series_occurrences() {
        LocalDate day = LocalDate.of(2026, 5, 4);
        LocalDateTime rangeStart = day.atStartOfDay();
        LocalDateTime rangeEnd = day.plusDays(1).atStartOfDay();

        User admin = user(Role.ADMIN, true, "Admin", "One");
        User requester = user(Role.CLUB_REPRESENTATIVE, true, "Club", "Rep");
        Hall hall = hall(HallType.PART_SMALL, "Halle A");

        BookingRequest pendingRequest = pendingRequest(
                hall,
                requester,
                LocalDateTime.of(2026, 5, 4, 12, 0),
                LocalDateTime.of(2026, 5, 4, 13, 0),
                "Anfrage"
        );

        de.hallenbelegung.application.domain.model.BookingSeriesRequest seriesRequest =
                new de.hallenbelegung.application.domain.model.BookingSeriesRequest(
                        UUID.randomUUID(),
                        "Serie",
                        "desc",
                        DayOfWeek.MONDAY,
                        LocalTime.of(18, 0),
                        LocalTime.of(19, 0),
                        LocalDate.of(2026, 5, 1),
                        LocalDate.of(2026, 5, 31),
                        BookingRequestStatus.PENDING,
                        null,
                        hall,
                        requester,
                        null,
                        Instant.now(),
                        Instant.now(),
                        null
                );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findByTimeRange(rangeStart, rangeEnd)).thenReturn(List.of());
        when(blockedTimeRepository.findAllByTimeRange(rangeStart, rangeEnd)).thenReturn(List.of());
        when(bookingRequestRepository.findByStatus(BookingRequestStatus.PENDING)).thenReturn(List.of(pendingRequest));
        when(bookingSeriesRequestRepository.findByStatus(BookingRequestStatus.PENDING)).thenReturn(List.of(seriesRequest));

        CalendarDayView result = service.getDay(day, admin.getId());

        assertEquals(2, result.entries().size());
        assertTrue(result.entries().stream().anyMatch(e -> e.type() == CalendarEntryType.BOOKING_REQUEST));
        assertTrue(result.entries().stream().anyMatch(e -> e.type() == CalendarEntryType.BOOKING_SERIES_REQUEST));
    }

    @Test
    void getDay_representative_sees_only_own_pending_requests() {
        LocalDate day = LocalDate.of(2026, 5, 4);
        LocalDateTime rangeStart = day.atStartOfDay();
        LocalDateTime rangeEnd = day.plusDays(1).atStartOfDay();

        User representative = user(Role.CLUB_REPRESENTATIVE, true, "Owner", "Rep");
        User otherRepresentative = user(Role.CLUB_REPRESENTATIVE, true, "Other", "Rep");
        Hall hall = hall(HallType.PART_SMALL, "Halle A");

        BookingRequest ownPending = pendingRequest(
                hall,
                representative,
                LocalDateTime.of(2026, 5, 4, 12, 0),
                LocalDateTime.of(2026, 5, 4, 13, 0),
                "Eigene Anfrage"
        );
        BookingRequest foreignPending = pendingRequest(
                hall,
                otherRepresentative,
                LocalDateTime.of(2026, 5, 4, 13, 0),
                LocalDateTime.of(2026, 5, 4, 14, 0),
                "Fremde Anfrage"
        );

        when(userRepository.findById(representative.getId())).thenReturn(Optional.of(representative));
        when(bookingRepository.findByTimeRange(rangeStart, rangeEnd)).thenReturn(List.of());
        when(blockedTimeRepository.findAllByTimeRange(rangeStart, rangeEnd)).thenReturn(List.of());
        when(bookingRequestRepository.findByStatus(BookingRequestStatus.PENDING)).thenReturn(List.of(ownPending, foreignPending));
        when(bookingSeriesRequestRepository.findByStatus(BookingRequestStatus.PENDING)).thenReturn(List.of());

        CalendarDayView result = service.getDay(day, representative.getId());

        assertEquals(1, result.entries().size());
        CalendarEntryView entry = result.entries().get(0);
        assertEquals("Eigene Anfrage", entry.title());
        assertTrue(entry.ownEntry());
        assertEquals(CalendarEntryType.BOOKING_REQUEST, entry.type());
    }

    @Test
    void getDay_marks_own_booking_entry_for_current_user() {
        LocalDate day = LocalDate.of(2026, 5, 4);
        LocalDateTime rangeStart = day.atStartOfDay();
        LocalDateTime rangeEnd = day.plusDays(1).atStartOfDay();

        User representative = user(Role.CLUB_REPRESENTATIVE, true, "Owner", "Rep");
        Hall hall = hall(HallType.PART_SMALL, "Halle A");

        Booking ownBooking = booking(
                hall,
                representative,
                LocalDateTime.of(2026, 5, 4, 17, 0),
                LocalDateTime.of(2026, 5, 4, 18, 0),
                "Eigene Buchung"
        );

        when(userRepository.findById(representative.getId())).thenReturn(Optional.of(representative));
        when(bookingRepository.findByTimeRange(rangeStart, rangeEnd)).thenReturn(List.of(ownBooking));
        when(blockedTimeRepository.findAllByTimeRange(rangeStart, rangeEnd)).thenReturn(List.of());
        when(bookingRequestRepository.findByStatus(BookingRequestStatus.PENDING)).thenReturn(List.of());
        when(bookingSeriesRequestRepository.findByStatus(BookingRequestStatus.PENDING)).thenReturn(List.of());

        CalendarDayView result = service.getDay(day, representative.getId());

        assertEquals(1, result.entries().size());
        assertTrue(result.entries().get(0).ownEntry());
        assertEquals("APPROVED", result.entries().get(0).status());
    }

    @Test
    void getDay_sorts_entries_by_start_time_ascending() {
        LocalDate day = LocalDate.of(2026, 5, 4);
        LocalDateTime rangeStart = day.atStartOfDay();
        LocalDateTime rangeEnd = day.plusDays(1).atStartOfDay();

        User representative = user(Role.CLUB_REPRESENTATIVE, true, "Owner", "Rep");
        Hall hall = hall(HallType.PART_SMALL, "Halle A");

        Booking later = booking(
                hall,
                representative,
                LocalDateTime.of(2026, 5, 4, 20, 0),
                LocalDateTime.of(2026, 5, 4, 21, 0),
                "Spaet"
        );
        Booking earlier = booking(
                hall,
                representative,
                LocalDateTime.of(2026, 5, 4, 9, 0),
                LocalDateTime.of(2026, 5, 4, 10, 0),
                "Frueh"
        );

        when(userRepository.findById(representative.getId())).thenReturn(Optional.of(representative));
        when(bookingRepository.findByTimeRange(rangeStart, rangeEnd)).thenReturn(List.of(later, earlier));
        when(blockedTimeRepository.findAllByTimeRange(rangeStart, rangeEnd)).thenReturn(List.of());
        when(bookingRequestRepository.findByStatus(BookingRequestStatus.PENDING)).thenReturn(List.of());
        when(bookingSeriesRequestRepository.findByStatus(BookingRequestStatus.PENDING)).thenReturn(List.of());

        CalendarDayView result = service.getDay(day, representative.getId());

        assertEquals(2, result.entries().size());
        assertEquals("Frueh", result.entries().get(0).title());
        assertEquals("Spaet", result.entries().get(1).title());
    }
}

