package de.hallenbelegung.application.domain.service;

import de.hallenbelegung.application.domain.exception.ForbiddenException;
import de.hallenbelegung.application.domain.exception.NotFoundException;
import de.hallenbelegung.application.domain.exception.ValidationException;
import de.hallenbelegung.application.domain.model.Booking;
import de.hallenbelegung.application.domain.model.BookingRequestStatus;
import de.hallenbelegung.application.domain.model.BookingSeries;
import de.hallenbelegung.application.domain.model.BookingSeriesRequest;
import de.hallenbelegung.application.domain.model.BookingStatus;
import de.hallenbelegung.application.domain.model.Hall;
import de.hallenbelegung.application.domain.model.HallType;
import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.out.BlockedTimeRepositoryPort;
import de.hallenbelegung.application.domain.port.out.BookingRepositoryPort;
import de.hallenbelegung.application.domain.port.out.BookingSeriesRepositoryPort;
import de.hallenbelegung.application.domain.port.out.BookingSeriesRequestRepositoryPort;
import de.hallenbelegung.application.domain.port.out.HallConfigPort;
import de.hallenbelegung.application.domain.port.out.HallRepositoryPort;
import de.hallenbelegung.application.domain.port.out.NotificationPort;
import de.hallenbelegung.application.domain.port.out.UserRepositoryPort;
import de.hallenbelegung.application.domain.view.BookingSeriesApproveResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BookingSeriesRequestServiceTest {

    private BookingSeriesRequestRepositoryPort bookingSeriesRequestRepository;
    private BookingSeriesRepositoryPort bookingSeriesRepository;
    private BookingRepositoryPort bookingRepository;
    private BlockedTimeRepositoryPort blockedTimeRepository;
    private UserRepositoryPort userRepository;
    private HallRepositoryPort hallRepository;
    private HallConfigPort config;
    private NotificationPort notificationPort;
    private Clock clock;

    private BookingSeriesRequestService service;

    @BeforeEach
    void setUp() {
        bookingSeriesRequestRepository = mock(BookingSeriesRequestRepositoryPort.class);
        bookingSeriesRepository = mock(BookingSeriesRepositoryPort.class);
        bookingRepository = mock(BookingRepositoryPort.class);
        blockedTimeRepository = mock(BlockedTimeRepositoryPort.class);
        userRepository = mock(UserRepositoryPort.class);
        hallRepository = mock(HallRepositoryPort.class);
        config = mock(HallConfigPort.class);
        notificationPort = mock(NotificationPort.class);

        clock = Clock.fixed(Instant.parse("2026-04-14T10:00:00Z"), ZoneId.of("Europe/Berlin"));

        when(config.bookingIntervalMinutes()).thenReturn(15);
        when(config.openingStart()).thenReturn(LocalTime.of(8, 0));
        when(config.openingEnd()).thenReturn(LocalTime.of(22, 0));

        service = new BookingSeriesRequestService(
                bookingSeriesRequestRepository,
                bookingSeriesRepository,
                bookingRepository,
                blockedTimeRepository,
                userRepository,
                hallRepository,
                config,
                clock,
                notificationPort
        );
    }

    private User user(Role role, boolean active, String email) {
        return new User(
                UUID.randomUUID(),
                "Max",
                "Mustermann",
                email,
                "hash",
                role,
                active,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z")
        );
    }

    private Hall hall(HallType hallType, boolean active, String name) {
        return new Hall(
                UUID.randomUUID(),
                name,
                "desc",
                active,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"),
                hallType
        );
    }

    private BookingSeriesRequest withId(BookingSeriesRequest request, UUID id) {
        return new BookingSeriesRequest(
                id,
                request.getTitle(),
                request.getDescription(),
                request.getWeekday(),
                request.getStartTime(),
                request.getEndTime(),
                request.getStartDate(),
                request.getEndDate(),
                request.getStatus(),
                request.getRejectionReason(),
                request.getHall(),
                request.getRequestedBy(),
                request.getProcessedBy(),
                request.getCreatedAt(),
                request.getUpdatedAt(),
                request.getProcessedAt()
        );
    }

    private BookingSeriesRequest requestWithCreatedAt(UUID id, User requester, Hall hall, Instant createdAt) {
        return new BookingSeriesRequest(
                id,
                "Serie",
                "desc",
                DayOfWeek.MONDAY,
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                BookingRequestStatus.PENDING,
                null,
                hall,
                requester,
                null,
                createdAt,
                createdAt,
                null
        );
    }

    private BookingSeries seriesWithId(BookingSeries input, UUID id) {
        return new BookingSeries(
                id,
                input.getTitle(),
                input.getDescription(),
                input.getWeekday(),
                input.getStartTime(),
                input.getEndTime(),
                input.getStartDate(),
                input.getEndDate(),
                input.getStatus(),
                input.getHall(),
                input.getResponsibleUser(),
                input.getCreatedBy(),
                input.getUpdatedBy(),
                input.getCancelledBy(),
                input.getCreatedAt(),
                input.getUpdatedAt(),
                input.getCancelledAt(),
                input.getCancelReason()
        );
    }

    private Booking bookingWithId(Booking booking, UUID id) {
        return new Booking(
                id,
                booking.getTitle(),
                booking.getDescription(),
                booking.getStartAt(),
                booking.getEndAt(),
                booking.getStatus(),
                booking.getParticipantCount(),
                booking.isConducted(),
                booking.getFeedbackComment(),
                booking.getHall(),
                booking.getResponsibleUser(),
                booking.getBookingSeries(),
                booking.getCreatedAt(),
                booking.getUpdatedAt(),
                booking.getCreatedBy(),
                booking.getUpdatedBy(),
                booking.getCancelledBy(),
                booking.getCancelledAt(),
                booking.getCancelReason()
        );
    }

    private Booking approvedBooking(Hall hall, User owner, LocalDateTime start, LocalDateTime end) {
        return new Booking(
                UUID.randomUUID(),
                "Bestehend",
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

    @Test
    void create_success_for_representative_and_notifies_admins() {
        User representative = user(Role.CLUB_REPRESENTATIVE, true, "rep@example.com");
        Hall hall = hall(HallType.PART_SMALL, true, "Halle A");

        when(userRepository.findById(representative.getId())).thenReturn(Optional.of(representative));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));
        when(bookingSeriesRequestRepository.save(any(BookingSeriesRequest.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0), UUID.randomUUID()));

        UUID createdId = service.create(
                representative.getId(),
                hall.getId(),
                "Jugendtraining",
                "Woechentlich",
                DayOfWeek.MONDAY,
                LocalTime.of(18, 0),
                LocalTime.of(19, 30),
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 5, 25)
        );

        assertNotNull(createdId);

        ArgumentCaptor<BookingSeriesRequest> captor = ArgumentCaptor.forClass(BookingSeriesRequest.class);
        verify(bookingSeriesRequestRepository).save(captor.capture());
        BookingSeriesRequest saved = captor.getValue();
        assertEquals("Jugendtraining", saved.getTitle());
        assertEquals(BookingRequestStatus.PENDING, saved.getStatus());
        assertEquals(representative.getId(), saved.getRequestedBy().getId());

        verify(notificationPort).notifyAdminsAboutNewBookingSeriesRequest(any(BookingSeriesRequest.class));
    }

    @Test
    void create_rejects_unknown_user() {
        UUID unknownUserId = UUID.randomUUID();

        when(userRepository.findById(unknownUserId)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> service.create(
                        unknownUserId,
                        UUID.randomUUID(),
                        "Serie",
                        "desc",
                        DayOfWeek.MONDAY,
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0),
                        LocalDate.of(2026, 4, 20),
                        LocalDate.of(2026, 5, 20)
                )
        );

        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void create_rejects_inactive_user() {
        User inactive = user(Role.CLUB_REPRESENTATIVE, false, "inactive@example.com");

        when(userRepository.findById(inactive.getId())).thenReturn(Optional.of(inactive));

        ForbiddenException ex = assertThrows(
                ForbiddenException.class,
                () -> service.create(
                        inactive.getId(),
                        UUID.randomUUID(),
                        "Serie",
                        "desc",
                        DayOfWeek.MONDAY,
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0),
                        LocalDate.of(2026, 4, 20),
                        LocalDate.of(2026, 5, 20)
                )
        );

        assertEquals("User inactive", ex.getMessage());
    }

    @Test
    void create_rejects_user_without_required_role_and_has_no_side_effects() {
        User plainUser = spy(user(Role.CLUB_REPRESENTATIVE, true, "plain@example.com"));
        doReturn(false).when(plainUser).isClubRepresentative();
        doReturn(false).when(plainUser).isAdmin();
        Hall hall = hall(HallType.PART_SMALL, true, "Halle A");

        when(userRepository.findById(plainUser.getId())).thenReturn(Optional.of(plainUser));

        ForbiddenException ex = assertThrows(
                ForbiddenException.class,
                () -> service.create(
                        plainUser.getId(),
                        hall.getId(),
                        "Serie",
                        "desc",
                        DayOfWeek.MONDAY,
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0),
                        LocalDate.of(2026, 4, 20),
                        LocalDate.of(2026, 5, 20)
                )
        );

        assertEquals("User not allowed to create booking series request", ex.getMessage());
        verify(hallRepository, never()).findById(any(UUID.class));
        verify(bookingSeriesRequestRepository, never()).save(any(BookingSeriesRequest.class));
        verify(notificationPort, never()).notifyAdminsAboutNewBookingSeriesRequest(any(BookingSeriesRequest.class));
    }

    @Test
    void create_rejects_unknown_hall() {
        User representative = user(Role.CLUB_REPRESENTATIVE, true, "rep@example.com");
        UUID hallId = UUID.randomUUID();

        when(userRepository.findById(representative.getId())).thenReturn(Optional.of(representative));
        when(hallRepository.findById(hallId)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> service.create(
                        representative.getId(),
                        hallId,
                        "Serie",
                        "desc",
                        DayOfWeek.MONDAY,
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0),
                        LocalDate.of(2026, 4, 20),
                        LocalDate.of(2026, 5, 20)
                )
        );

        assertEquals("Hall not found", ex.getMessage());
    }

    @Test
    void create_rejects_inactive_hall() {
        User representative = user(Role.CLUB_REPRESENTATIVE, true, "rep@example.com");
        Hall inactiveHall = hall(HallType.PART_SMALL, false, "Halle A");

        when(userRepository.findById(representative.getId())).thenReturn(Optional.of(representative));
        when(hallRepository.findById(inactiveHall.getId())).thenReturn(Optional.of(inactiveHall));

        ForbiddenException ex = assertThrows(
                ForbiddenException.class,
                () -> service.create(
                        representative.getId(),
                        inactiveHall.getId(),
                        "Serie",
                        "desc",
                        DayOfWeek.MONDAY,
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0),
                        LocalDate.of(2026, 4, 20),
                        LocalDate.of(2026, 5, 20)
                )
        );

        assertEquals("Hall inactive", ex.getMessage());
    }

    @Test
    void create_rejects_blank_title() {
        User representative = user(Role.CLUB_REPRESENTATIVE, true, "rep@example.com");
        Hall hall = hall(HallType.PART_SMALL, true, "Halle A");

        when(userRepository.findById(representative.getId())).thenReturn(Optional.of(representative));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.create(
                        representative.getId(),
                        hall.getId(),
                        "   ",
                        "desc",
                        DayOfWeek.MONDAY,
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0),
                        LocalDate.of(2026, 4, 20),
                        LocalDate.of(2026, 5, 20)
                )
        );

        assertEquals("Title required", ex.getMessage());
    }

    @Test
    void create_rejects_missing_times() {
        User representative = user(Role.CLUB_REPRESENTATIVE, true, "rep@example.com");
        Hall hall = hall(HallType.PART_SMALL, true, "Halle A");

        when(userRepository.findById(representative.getId())).thenReturn(Optional.of(representative));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.create(
                        representative.getId(),
                        hall.getId(),
                        "Serie",
                        "desc",
                        DayOfWeek.MONDAY,
                        null,
                        LocalTime.of(11, 0),
                        LocalDate.of(2026, 4, 20),
                        LocalDate.of(2026, 5, 20)
                )
        );

        assertEquals("Start time and end time are required", ex.getMessage());
    }

    @Test
    void create_rejects_start_time_equal_end_time() {
        User representative = user(Role.CLUB_REPRESENTATIVE, true, "rep@example.com");
        Hall hall = hall(HallType.PART_SMALL, true, "Halle A");

        when(userRepository.findById(representative.getId())).thenReturn(Optional.of(representative));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.create(
                        representative.getId(),
                        hall.getId(),
                        "Serie",
                        "desc",
                        DayOfWeek.MONDAY,
                        LocalTime.of(10, 0),
                        LocalTime.of(10, 0),
                        LocalDate.of(2026, 4, 20),
                        LocalDate.of(2026, 5, 20)
                )
        );

        assertEquals("Start time must be before end time", ex.getMessage());
    }

    @Test
    void create_rejects_invalid_date_range() {
        User representative = user(Role.CLUB_REPRESENTATIVE, true, "rep@example.com");
        Hall hall = hall(HallType.PART_SMALL, true, "Halle A");

        when(userRepository.findById(representative.getId())).thenReturn(Optional.of(representative));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.create(
                        representative.getId(),
                        hall.getId(),
                        "Serie",
                        "desc",
                        DayOfWeek.MONDAY,
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0),
                        LocalDate.of(2026, 5, 21),
                        LocalDate.of(2026, 5, 20)
                )
        );

        assertEquals("Start date must be before or equal to end date", ex.getMessage());
    }

    @Test
    void create_rejects_past_start_date() {
        User representative = user(Role.CLUB_REPRESENTATIVE, true, "rep@example.com");
        Hall hall = hall(HallType.PART_SMALL, true, "Halle A");

        when(userRepository.findById(representative.getId())).thenReturn(Optional.of(representative));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.create(
                        representative.getId(),
                        hall.getId(),
                        "Serie",
                        "desc",
                        DayOfWeek.MONDAY,
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0),
                        LocalDate.of(2026, 4, 13),
                        LocalDate.of(2026, 5, 20)
                )
        );

        assertEquals("Cannot create series request in the past", ex.getMessage());
    }

    @Test
    void create_rejects_more_than_one_year_ahead() {
        User representative = user(Role.CLUB_REPRESENTATIVE, true, "rep@example.com");
        Hall hall = hall(HallType.PART_SMALL, true, "Halle A");

        when(userRepository.findById(representative.getId())).thenReturn(Optional.of(representative));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.create(
                        representative.getId(),
                        hall.getId(),
                        "Serie",
                        "desc",
                        DayOfWeek.MONDAY,
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0),
                        LocalDate.of(2027, 4, 15),
                        LocalDate.of(2027, 5, 20)
                )
        );

        assertEquals("Series request may be created at most one year in advance", ex.getMessage());
    }

    @Test
    void create_rejects_not_on_15_minute_grid() {
        User representative = user(Role.CLUB_REPRESENTATIVE, true, "rep@example.com");
        Hall hall = hall(HallType.PART_SMALL, true, "Halle A");

        when(userRepository.findById(representative.getId())).thenReturn(Optional.of(representative));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.create(
                        representative.getId(),
                        hall.getId(),
                        "Serie",
                        "desc",
                        DayOfWeek.MONDAY,
                        LocalTime.of(10, 5),
                        LocalTime.of(11, 0),
                        LocalDate.of(2026, 4, 20),
                        LocalDate.of(2026, 5, 20)
                )
        );

        assertEquals("Not on valid time grid", ex.getMessage());
    }

    @Test
    void create_rejects_seconds_or_nanoseconds() {
        User representative = user(Role.CLUB_REPRESENTATIVE, true, "rep@example.com");
        Hall hall = hall(HallType.PART_SMALL, true, "Halle A");

        when(userRepository.findById(representative.getId())).thenReturn(Optional.of(representative));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.create(
                        representative.getId(),
                        hall.getId(),
                        "Serie",
                        "desc",
                        DayOfWeek.MONDAY,
                        LocalTime.of(10, 0, 1),
                        LocalTime.of(11, 0),
                        LocalDate.of(2026, 4, 20),
                        LocalDate.of(2026, 5, 20)
                )
        );

        assertEquals("Seconds and nanoseconds are not allowed", ex.getMessage());
    }

    @Test
    void create_rejects_outside_opening_hours() {
        User representative = user(Role.CLUB_REPRESENTATIVE, true, "rep@example.com");
        Hall hall = hall(HallType.PART_SMALL, true, "Halle A");

        when(userRepository.findById(representative.getId())).thenReturn(Optional.of(representative));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.create(
                        representative.getId(),
                        hall.getId(),
                        "Serie",
                        "desc",
                        DayOfWeek.MONDAY,
                        LocalTime.of(7, 45),
                        LocalTime.of(9, 0),
                        LocalDate.of(2026, 4, 20),
                        LocalDate.of(2026, 5, 20)
                )
        );

        assertEquals("Outside opening hours", ex.getMessage());
    }

    @Test
    void create_rejects_when_date_range_contains_no_occurrence() {
        User representative = user(Role.CLUB_REPRESENTATIVE, true, "rep@example.com");
        Hall hall = hall(HallType.PART_SMALL, true, "Halle A");

        when(userRepository.findById(representative.getId())).thenReturn(Optional.of(representative));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.create(
                        representative.getId(),
                        hall.getId(),
                        "Serie",
                        "desc",
                        DayOfWeek.MONDAY,
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0),
                        LocalDate.of(2026, 4, 14),
                        LocalDate.of(2026, 4, 15)
                )
        );

        assertEquals("Series request does not contain any valid appointment", ex.getMessage());
    }

    @Test
    void approve_rejects_non_admin() {
        User representative = user(Role.CLUB_REPRESENTATIVE, true, "rep@example.com");

        when(userRepository.findById(representative.getId())).thenReturn(Optional.of(representative));

        ForbiddenException ex = assertThrows(
                ForbiddenException.class,
                () -> service.approve(representative.getId(), UUID.randomUUID())
        );

        assertEquals("User not allowed to approve booking series requests", ex.getMessage());
    }

    @Test
    void approve_rejects_unknown_request_and_has_no_side_effects() {
        User admin = user(Role.ADMIN, true, "admin@example.com");
        UUID unknownRequestId = UUID.randomUUID();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingSeriesRequestRepository.findById(unknownRequestId)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> service.approve(admin.getId(), unknownRequestId)
        );

        assertEquals("Booking series request not found", ex.getMessage());
        verify(bookingSeriesRepository, never()).save(any(BookingSeries.class));
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(notificationPort, never()).notifyRequesterAboutBookingSeriesRequestApproved(any(BookingSeriesRequest.class), any(BookingSeries.class));
    }

    @Test
    void approve_rejects_non_pending_request() {
        User admin = user(Role.ADMIN, true, "admin@example.com");
        Hall hall = hall(HallType.PART_SMALL, true, "Halle A");

        BookingSeriesRequest request = requestWithCreatedAt(UUID.randomUUID(), admin, hall, Instant.now());
        request.approve(admin);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingSeriesRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.approve(admin.getId(), request.getId())
        );

        assertEquals("Booking series request is not open", ex.getMessage());
    }

    @Test
    void approve_rejects_inactive_hall() {
        User admin = user(Role.ADMIN, true, "admin@example.com");
        User requester = user(Role.CLUB_REPRESENTATIVE, true, "rep@example.com");
        Hall inactiveHall = hall(HallType.PART_SMALL, false, "Halle A");
        BookingSeriesRequest request = requestWithCreatedAt(UUID.randomUUID(), requester, inactiveHall, Instant.now());

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingSeriesRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        ForbiddenException ex = assertThrows(
                ForbiddenException.class,
                () -> service.approve(admin.getId(), request.getId())
        );

        assertEquals("Hall inactive", ex.getMessage());
    }

    @Test
    void approve_rejects_when_all_occurrences_conflict() {
        User admin = user(Role.ADMIN, true, "admin@example.com");
        User requester = user(Role.CLUB_REPRESENTATIVE, true, "rep@example.com");
        Hall fullHall = hall(HallType.FULL, true, "Gesamthalle");

        BookingSeriesRequest request = new BookingSeriesRequest(
                UUID.randomUUID(),
                "Serie",
                "desc",
                DayOfWeek.MONDAY,
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                BookingRequestStatus.PENDING,
                null,
                fullHall,
                requester,
                null,
                Instant.now(),
                Instant.now(),
                null
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingSeriesRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(bookingRepository.findByTimeRange(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(approvedBooking(fullHall, requester,
                        LocalDateTime.of(2026, 5, 4, 10, 0),
                        LocalDateTime.of(2026, 5, 4, 11, 0))));

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.approve(admin.getId(), request.getId())
        );

        assertEquals("No conflict-free appointments available for this series request", ex.getMessage());
        verify(bookingSeriesRepository, never()).save(any(BookingSeries.class));
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(bookingSeriesRequestRepository, never()).save(any(BookingSeriesRequest.class));
        verify(notificationPort, never()).notifyRequesterAboutBookingSeriesRequestApproved(any(BookingSeriesRequest.class), any(BookingSeries.class));
    }

    @Test
    void approve_creates_only_conflict_free_occurrences_and_reports_skipped_dates() {
        User admin = user(Role.ADMIN, true, "admin@example.com");
        User requester = user(Role.CLUB_REPRESENTATIVE, true, "rep@example.com");
        Hall partHall = hall(HallType.PART_SMALL, true, "Halle A");

        BookingSeriesRequest request = new BookingSeriesRequest(
                UUID.randomUUID(),
                "Serie",
                "desc",
                DayOfWeek.MONDAY,
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 20),
                BookingRequestStatus.PENDING,
                null,
                partHall,
                requester,
                null,
                Instant.now(),
                Instant.now(),
                null
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingSeriesRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(hallRepository.findAllActive()).thenReturn(List.of(partHall));
        when(bookingSeriesRepository.findByHallId(partHall.getId())).thenReturn(List.of());
        when(blockedTimeRepository.findByHallIdAndTimeRange(any(UUID.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(blockedTimeRepository.findAllByTimeRange(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        doAnswer(invocation -> {
            LocalDateTime start = invocation.getArgument(1);
            if (start.toLocalDate().equals(LocalDate.of(2026, 5, 4))) {
                return List.of(approvedBooking(partHall, requester, start, start.plusHours(1)));
            }
            return List.of();
        }).when(bookingRepository).findByHallIdAndTimeRange(eq(partHall.getId()), any(LocalDateTime.class), any(LocalDateTime.class));

        when(bookingRepository.findByTimeRange(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());

        when(bookingSeriesRepository.save(any(BookingSeries.class)))
                .thenAnswer(invocation -> seriesWithId(invocation.getArgument(0), UUID.randomUUID()));

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> bookingWithId(invocation.getArgument(0), UUID.randomUUID()));

        when(bookingSeriesRequestRepository.save(any(BookingSeriesRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BookingSeriesApproveResult result = service.approve(admin.getId(), request.getId());

        assertEquals(3, result.skippedOccurrences.size() + result.createdBookingIds.size());
        assertEquals(2, result.createdBookingIds.size());
        assertEquals(1, result.skippedOccurrences.size());
        assertTrue(result.skippedOccurrences.contains(LocalDate.of(2026, 5, 4)));

        verify(notificationPort).notifyRequesterAboutBookingSeriesRequestApproved(any(BookingSeriesRequest.class), any(BookingSeries.class));
    }

    @Test
    void reject_updates_request_and_notifies_requester() {
        User admin = user(Role.ADMIN, true, "admin@example.com");
        User requester = user(Role.CLUB_REPRESENTATIVE, true, "rep@example.com");
        Hall hall = hall(HallType.PART_SMALL, true, "Halle A");

        BookingSeriesRequest request = requestWithCreatedAt(UUID.randomUUID(), requester, hall, Instant.now());

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingSeriesRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(bookingSeriesRequestRepository.save(any(BookingSeriesRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.reject(admin.getId(), request.getId(), "Termin kollidiert mit Veranstaltung");

        assertTrue(request.isRejected());
        assertEquals("Termin kollidiert mit Veranstaltung", request.getRejectionReason());
        assertEquals(admin.getId(), request.getProcessedBy().getId());

        verify(notificationPort).notifyRequesterAboutBookingSeriesRequestRejected(request, "Termin kollidiert mit Veranstaltung");
    }

    @Test
    void reject_rejects_non_admin_and_has_no_side_effects() {
        User representative = user(Role.CLUB_REPRESENTATIVE, true, "rep@example.com");
        UUID requestId = UUID.randomUUID();

        when(userRepository.findById(representative.getId())).thenReturn(Optional.of(representative));

        ForbiddenException ex = assertThrows(
                ForbiddenException.class,
                () -> service.reject(representative.getId(), requestId, "Nope")
        );

        assertEquals("User not allowed to reject booking series requests", ex.getMessage());
        verify(bookingSeriesRequestRepository, never()).findById(any(UUID.class));
        verify(bookingSeriesRequestRepository, never()).save(any(BookingSeriesRequest.class));
        verify(notificationPort, never()).notifyRequesterAboutBookingSeriesRequestRejected(any(BookingSeriesRequest.class), any(String.class));
    }

    @Test
    void reject_rejects_unknown_request_and_has_no_side_effects() {
        User admin = user(Role.ADMIN, true, "admin@example.com");
        UUID requestId = UUID.randomUUID();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingSeriesRequestRepository.findById(requestId)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> service.reject(admin.getId(), requestId, "Nope")
        );

        assertEquals("Booking series request not found", ex.getMessage());
        verify(bookingSeriesRequestRepository, never()).save(any(BookingSeriesRequest.class));
        verify(notificationPort, never()).notifyRequesterAboutBookingSeriesRequestRejected(any(BookingSeriesRequest.class), any(String.class));
    }

    @Test
    void getOpenRequests_requires_admin_and_sorts_desc() {
        User admin = user(Role.ADMIN, true, "admin@example.com");
        User requester = user(Role.CLUB_REPRESENTATIVE, true, "rep@example.com");
        Hall hall = hall(HallType.PART_SMALL, true, "Halle A");

        BookingSeriesRequest older = requestWithCreatedAt(
                UUID.randomUUID(),
                requester,
                hall,
                Instant.parse("2026-01-01T00:00:00Z")
        );
        BookingSeriesRequest newer = requestWithCreatedAt(
                UUID.randomUUID(),
                requester,
                hall,
                Instant.parse("2026-03-01T00:00:00Z")
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingSeriesRequestRepository.findByStatus(BookingRequestStatus.PENDING)).thenReturn(List.of(older, newer));

        List<BookingSeriesRequest> result = service.getOpenRequests(admin.getId());

        assertEquals(2, result.size());
        assertEquals(newer.getId(), result.get(0).getId());
        assertEquals(older.getId(), result.get(1).getId());
    }

    @Test
    void getOpenRequests_rejects_non_admin() {
        User representative = user(Role.CLUB_REPRESENTATIVE, true, "rep@example.com");

        when(userRepository.findById(representative.getId())).thenReturn(Optional.of(representative));

        ForbiddenException ex = assertThrows(
                ForbiddenException.class,
                () -> service.getOpenRequests(representative.getId())
        );

        assertEquals("User not allowed to view open booking series requests", ex.getMessage());
    }

    @Test
    void getAllRequests_sorts_descending_by_created_at() {
        User admin = user(Role.ADMIN, true, "admin@example.com");
        User requester = user(Role.CLUB_REPRESENTATIVE, true, "rep@example.com");
        Hall hall = hall(HallType.PART_SMALL, true, "Halle A");

        BookingSeriesRequest older = requestWithCreatedAt(
                UUID.randomUUID(),
                requester,
                hall,
                Instant.parse("2026-01-01T00:00:00Z")
        );
        BookingSeriesRequest newer = requestWithCreatedAt(
                UUID.randomUUID(),
                requester,
                hall,
                Instant.parse("2026-03-01T00:00:00Z")
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingSeriesRequestRepository.findAll()).thenReturn(List.of(older, newer));

        List<BookingSeriesRequest> result = service.getAllRequests(admin.getId());

        assertEquals(newer.getId(), result.get(0).getId());
        assertEquals(older.getId(), result.get(1).getId());
    }

    @Test
    void getRequestsByUser_returns_only_own_requests_sorted_desc() {
        User requester = user(Role.CLUB_REPRESENTATIVE, true, "rep@example.com");
        Hall hall = hall(HallType.PART_SMALL, true, "Halle A");

        BookingSeriesRequest older = requestWithCreatedAt(
                UUID.randomUUID(),
                requester,
                hall,
                Instant.parse("2026-01-01T00:00:00Z")
        );
        BookingSeriesRequest newer = requestWithCreatedAt(
                UUID.randomUUID(),
                requester,
                hall,
                Instant.parse("2026-02-01T00:00:00Z")
        );

        when(userRepository.findById(requester.getId())).thenReturn(Optional.of(requester));
        when(bookingSeriesRequestRepository.findByRequestedByUserId(requester.getId())).thenReturn(List.of(older, newer));

        List<BookingSeriesRequest> result = service.getRequestsByUser(requester.getId());

        assertEquals(2, result.size());
        assertEquals(newer.getId(), result.get(0).getId());
    }

    @Test
    void getById_allows_admin_and_owner_but_rejects_other_user() {
        User admin = user(Role.ADMIN, true, "admin@example.com");
        User owner = user(Role.CLUB_REPRESENTATIVE, true, "owner@example.com");
        User other = user(Role.CLUB_REPRESENTATIVE, true, "other@example.com");
        Hall hall = hall(HallType.PART_SMALL, true, "Halle A");

        BookingSeriesRequest request = requestWithCreatedAt(UUID.randomUUID(), owner, hall, Instant.now());

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(userRepository.findById(other.getId())).thenReturn(Optional.of(other));
        when(bookingSeriesRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        BookingSeriesRequest asAdmin = service.getById(admin.getId(), request.getId());
        BookingSeriesRequest asOwner = service.getById(owner.getId(), request.getId());

        assertEquals(request.getId(), asAdmin.getId());
        assertEquals(request.getId(), asOwner.getId());

        ForbiddenException ex = assertThrows(
                ForbiddenException.class,
                () -> service.getById(other.getId(), request.getId())
        );

        assertEquals("User not allowed to view this booking series request", ex.getMessage());
        assertFalse(request.isApproved());
    }
}


