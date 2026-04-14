package de.hallenbelegung.application.domain.service;

import de.hallenbelegung.application.domain.exception.BookingConflictException;
import de.hallenbelegung.application.domain.exception.ForbiddenException;
import de.hallenbelegung.application.domain.exception.NotFoundException;
import de.hallenbelegung.application.domain.exception.ValidationException;
import de.hallenbelegung.application.domain.model.Booking;
import de.hallenbelegung.application.domain.model.BookingRequest;
import de.hallenbelegung.application.domain.model.BookingRequestStatus;
import de.hallenbelegung.application.domain.model.Hall;
import de.hallenbelegung.application.domain.model.HallType;
import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.out.BlockedTimeRepositoryPort;
import de.hallenbelegung.application.domain.port.out.BookingRepositoryPort;
import de.hallenbelegung.application.domain.port.out.BookingRequestRepositoryPort;
import de.hallenbelegung.application.domain.port.out.HallConfigPort;
import de.hallenbelegung.application.domain.port.out.HallRepositoryPort;
import de.hallenbelegung.application.domain.port.out.NotificationPort;
import de.hallenbelegung.application.domain.port.out.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BookingRequestServiceTest {

    private BookingRequestRepositoryPort bookingRequestRepository;
    private BookingRepositoryPort bookingRepository;
    private BlockedTimeRepositoryPort blockedTimeRepository;
    private UserRepositoryPort userRepository;
    private HallRepositoryPort hallRepository;
    private HallConfigPort config;
    private NotificationPort notificationPort;
    private Clock clock;

    private BookingRequestService service;

    @BeforeEach
    void setUp() {
        bookingRequestRepository = mock(BookingRequestRepositoryPort.class);
        bookingRepository = mock(BookingRepositoryPort.class);
        blockedTimeRepository = mock(BlockedTimeRepositoryPort.class);
        userRepository = mock(UserRepositoryPort.class);
        hallRepository = mock(HallRepositoryPort.class);
        config = mock(HallConfigPort.class);
        notificationPort = mock(NotificationPort.class);

        clock = Clock.fixed(
                Instant.parse("2026-04-14T10:00:00Z"),
                ZoneId.of("Europe/Berlin")
        );

        when(config.bookingIntervalMinutes()).thenReturn(15);
        when(config.openingStart()).thenReturn(LocalTime.of(8, 0));
        when(config.openingEnd()).thenReturn(LocalTime.of(22, 0));

        service = new BookingRequestService(
                bookingRequestRepository,
                bookingRepository,
                blockedTimeRepository,
                userRepository,
                hallRepository,
                config,
                clock,
                notificationPort
        );
    }

    private User createAdmin() {
        return new User(
                UUID.randomUUID(),
                "Admin",
                "User",
                "admin@example.com",
                "hash",
                Role.ADMIN,
                true,
                Instant.now(),
                Instant.now()
        );
    }

    private User createRepresentative() {
        return new User(
                UUID.randomUUID(),
                "Club",
                "Rep",
                "rep@example.com",
                "hash",
                Role.CLUB_REPRESENTATIVE,
                true,
                Instant.now(),
                Instant.now()
        );
    }

    private User createInactiveRepresentative() {
        return new User(
                UUID.randomUUID(),
                "Inactive",
                "Rep",
                "inactive@example.com",
                "hash",
                Role.CLUB_REPRESENTATIVE,
                false,
                Instant.now(),
                Instant.now()
        );
    }

    private Hall createFullHall() {
        return new Hall(
                UUID.randomUUID(),
                "Gesamthalle",
                "Komplette Halle",
                true,
                Instant.now(),
                Instant.now(),
                HallType.FULL
        );
    }

    private Hall createPartHallA() {
        return new Hall(
                UUID.randomUUID(),
                "Halle A",
                "Teilhalle A",
                true,
                Instant.now(),
                Instant.now(),
                HallType.PART_SMALL
        );
    }

    private Hall createInactiveHall() {
        return new Hall(
                UUID.randomUUID(),
                "Inactive Hall",
                "desc",
                false,
                Instant.now(),
                Instant.now(),
                HallType.PART_SMALL
        );
    }

    private BookingRequest withId(BookingRequest request, UUID id) {
        return new BookingRequest(
                id,
                request.getTitle(),
                request.getDescription(),
                request.getStartAt(),
                request.getEndAt(),
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

    private Booking createApprovedBooking(Hall hall, User user, LocalDateTime start, LocalDateTime end) {
        return new Booking(
                UUID.randomUUID(),
                "Existing Booking",
                "desc",
                start,
                end,
                de.hallenbelegung.application.domain.model.BookingStatus.APPROVED,
                null,
                false,
                null,
                hall,
                user,
                null,
                Instant.now(),
                Instant.now(),
                user,
                null,
                null,
                null,
                null
        );
    }

    @Test
    void create_request_success_for_future_slot() {
        User user = createRepresentative();
        Hall hall = createPartHallA();

        LocalDateTime start = LocalDateTime.of(2026, 4, 20, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 20, 11, 0);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));
        when(bookingRepository.findByHallIdAndTimeRange(hall.getId(), start, end)).thenReturn(List.of());
        when(bookingRepository.findByTimeRange(start, end)).thenReturn(List.of());
        when(blockedTimeRepository.findByHallIdAndTimeRange(hall.getId(), start, end)).thenReturn(List.of());
        when(blockedTimeRepository.findAllByTimeRange(start, end)).thenReturn(List.of());

        when(bookingRequestRepository.save(any(BookingRequest.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0), UUID.randomUUID()));

        UUID requestId = service.create(
                user.getId(),
                hall.getId(),
                "Volleyball Jugend",
                "Training",
                start,
                end
        );

        assertNotNull(requestId);

        ArgumentCaptor<BookingRequest> captor = ArgumentCaptor.forClass(BookingRequest.class);
        verify(bookingRequestRepository).save(captor.capture());

        BookingRequest saved = captor.getValue();
        assertEquals("Volleyball Jugend", saved.getTitle());
        assertEquals("Training", saved.getDescription());
        assertEquals(start, saved.getStartAt());
        assertEquals(end, saved.getEndAt());
        assertEquals(hall.getId(), saved.getHall().getId());
        assertEquals(user.getId(), saved.getRequestedBy().getId());
        assertEquals(BookingRequestStatus.PENDING, saved.getStatus());

        verify(notificationPort).notifyAdminsAboutNewBookingRequest(any(BookingRequest.class));
    }

    @Test
    void create_request_rejects_null_start_time() {
        User user = createRepresentative();
        Hall hall = createPartHallA();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.create(
                        user.getId(),
                        hall.getId(),
                        "Test",
                        "desc",
                        null,
                        LocalDateTime.of(2026, 4, 20, 11, 0)
                )
        );

        assertEquals("Start time and end time are required", exception.getMessage());
    }

    @Test
    void create_request_rejects_seconds_and_nanos() {
        User user = createRepresentative();
        Hall hall = createPartHallA();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.create(
                        user.getId(),
                        hall.getId(),
                        "Test",
                        "desc",
                        LocalDateTime.of(2026, 4, 20, 10, 0, 1),
                        LocalDateTime.of(2026, 4, 20, 11, 0, 0, 1)
                )
        );

        assertEquals("Seconds and nanoseconds are not allowed", exception.getMessage());
    }

    @Test
    void create_request_rejects_non_representative_non_admin_user() {
        User user = spy(createRepresentative());
        doReturn(false).when(user).isClubRepresentative();
        doReturn(false).when(user).isAdmin();
        Hall hall = createPartHallA();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.create(
                        user.getId(),
                        hall.getId(),
                        "Test",
                        "desc",
                        LocalDateTime.of(2026, 4, 20, 10, 0),
                        LocalDateTime.of(2026, 4, 20, 11, 0)
                )
        );

        assertEquals("User not allowed to create booking request", exception.getMessage());
    }

    @Test
    void create_request_allows_admin_user() {
        User admin = createAdmin();
        Hall hall = createPartHallA();
        LocalDateTime start = LocalDateTime.of(2026, 4, 20, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 20, 11, 0);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));
        when(bookingRepository.findByHallIdAndTimeRange(hall.getId(), start, end)).thenReturn(List.of());
        when(bookingRepository.findByTimeRange(start, end)).thenReturn(List.of());
        when(blockedTimeRepository.findByHallIdAndTimeRange(hall.getId(), start, end)).thenReturn(List.of());
        when(blockedTimeRepository.findAllByTimeRange(start, end)).thenReturn(List.of());
        when(bookingRequestRepository.save(any(BookingRequest.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0), UUID.randomUUID()));

        UUID requestId = service.create(admin.getId(), hall.getId(), "Admin Booking", "desc", start, end);

        assertNotNull(requestId);
        verify(notificationPort).notifyAdminsAboutNewBookingRequest(any(BookingRequest.class));
    }

    @Test
    void create_request_accepts_exact_opening_boundaries() {
        User user = createRepresentative();
        Hall hall = createPartHallA();

        LocalDateTime start = LocalDateTime.of(2026, 4, 20, 8, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 20, 22, 0);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));
        when(bookingRepository.findByHallIdAndTimeRange(hall.getId(), start, end)).thenReturn(List.of());
        when(bookingRepository.findByTimeRange(start, end)).thenReturn(List.of());
        when(blockedTimeRepository.findByHallIdAndTimeRange(hall.getId(), start, end)).thenReturn(List.of());
        when(blockedTimeRepository.findAllByTimeRange(start, end)).thenReturn(List.of());
        when(bookingRequestRepository.save(any(BookingRequest.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0), UUID.randomUUID()));

        UUID requestId = service.create(
                user.getId(),
                hall.getId(),
                "Boundary Booking",
                "desc",
                start,
                end
        );

        assertNotNull(requestId);
        verify(bookingRequestRepository).save(any(BookingRequest.class));
    }

    @Test
    void create_request_rejects_unknown_user() {
        UUID userId = UUID.randomUUID();
        UUID hallId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.create(
                        userId,
                        hallId,
                        "Test",
                        "desc",
                        LocalDateTime.of(2026, 4, 20, 10, 0),
                        LocalDateTime.of(2026, 4, 20, 11, 0)
                )
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void create_request_rejects_inactive_user() {
        User user = createInactiveRepresentative();
        Hall hall = createPartHallA();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.create(
                        user.getId(),
                        hall.getId(),
                        "Test",
                        "desc",
                        LocalDateTime.of(2026, 4, 20, 10, 0),
                        LocalDateTime.of(2026, 4, 20, 11, 0)
                )
        );

        assertEquals("User inactive", exception.getMessage());
    }

    @Test
    void create_request_rejects_unknown_hall() {
        User user = createRepresentative();
        UUID hallId = UUID.randomUUID();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(hallRepository.findById(hallId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.create(
                        user.getId(),
                        hallId,
                        "Test",
                        "desc",
                        LocalDateTime.of(2026, 4, 20, 10, 0),
                        LocalDateTime.of(2026, 4, 20, 11, 0)
                )
        );

        assertEquals("Hall not found", exception.getMessage());
    }

    @Test
    void create_request_rejects_blank_title() {
        User user = createRepresentative();
        Hall hall = createPartHallA();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.create(
                        user.getId(),
                        hall.getId(),
                        "   ",
                        "desc",
                        LocalDateTime.of(2026, 4, 20, 10, 0),
                        LocalDateTime.of(2026, 4, 20, 11, 0)
                )
        );

        assertEquals("Title required", exception.getMessage());
    }

    @Test
    void create_request_rejects_start_after_end() {
        User user = createRepresentative();
        Hall hall = createPartHallA();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.create(
                        user.getId(),
                        hall.getId(),
                        "Test",
                        "desc",
                        LocalDateTime.of(2026, 4, 20, 12, 0),
                        LocalDateTime.of(2026, 4, 20, 11, 0)
                )
        );

        assertEquals("Start must be before end", exception.getMessage());
    }

    @Test
    void create_request_rejects_past_booking() {
        User user = createRepresentative();
        Hall hall = createPartHallA();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.create(
                        user.getId(),
                        hall.getId(),
                        "Test",
                        "desc",
                        LocalDateTime.of(2026, 4, 13, 10, 0),
                        LocalDateTime.of(2026, 4, 13, 11, 0)
                )
        );

        assertEquals("Cannot book in the past", exception.getMessage());
    }

    @Test
    void create_request_rejects_more_than_one_year_in_future() {
        User user = createRepresentative();
        Hall hall = createPartHallA();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.create(
                        user.getId(),
                        hall.getId(),
                        "Test",
                        "desc",
                        LocalDateTime.of(2027, 4, 15, 10, 0),
                        LocalDateTime.of(2027, 4, 15, 11, 0)
                )
        );

        assertEquals("Booking request must not be more than one year in advance", exception.getMessage());
    }

    @Test
    void create_request_rejects_outside_opening_hours() {
        User user = createRepresentative();
        Hall hall = createPartHallA();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.create(
                        user.getId(),
                        hall.getId(),
                        "Test",
                        "desc",
                        LocalDateTime.of(2026, 4, 20, 7, 45),
                        LocalDateTime.of(2026, 4, 20, 9, 0)
                )
        );

        assertEquals("Outside opening hours", exception.getMessage());
    }

    @Test
    void create_request_rejects_non_15_minute_grid() {
        User user = createRepresentative();
        Hall hall = createPartHallA();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.create(
                        user.getId(),
                        hall.getId(),
                        "Test",
                        "desc",
                        LocalDateTime.of(2026, 4, 20, 10, 10),
                        LocalDateTime.of(2026, 4, 20, 11, 0)
                )
        );

        assertEquals("Not on valid time grid", exception.getMessage());
    }

    @Test
    void create_request_rejects_conflict_with_same_hall_booking() {
        User user = createRepresentative();
        Hall hall = createPartHallA();
        LocalDateTime start = LocalDateTime.of(2026, 4, 20, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 20, 11, 0);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));
        when(bookingRepository.findByHallIdAndTimeRange(hall.getId(), start, end))
                .thenReturn(List.of(createApprovedBooking(hall, user, start, end)));

        BookingConflictException exception = assertThrows(
                BookingConflictException.class,
                () -> service.create(user.getId(), hall.getId(), "Test", "desc", start, end)
        );

        assertEquals("Conflict with existing booking", exception.getMessage());
    }

    @Test
    void create_request_rejects_conflict_with_full_hall_booking() {
        User user = createRepresentative();
        Hall requestedHall = createPartHallA();
        Hall fullHall = createFullHall();
        LocalDateTime start = LocalDateTime.of(2026, 4, 20, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 20, 11, 0);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(hallRepository.findById(requestedHall.getId())).thenReturn(Optional.of(requestedHall));
        when(bookingRepository.findByHallIdAndTimeRange(requestedHall.getId(), start, end)).thenReturn(List.of());
        when(bookingRepository.findByTimeRange(start, end))
                .thenReturn(List.of(createApprovedBooking(fullHall, user, start, end)));

        BookingConflictException exception = assertThrows(
                BookingConflictException.class,
                () -> service.create(user.getId(), requestedHall.getId(), "Test", "desc", start, end)
        );

        assertEquals("Conflict with full hall booking", exception.getMessage());
    }

    @Test
    void create_request_ignores_cancelled_full_hall_booking_conflicts() {
        User user = createRepresentative();
        Hall requestedHall = createPartHallA();
        Hall fullHall = createFullHall();
        LocalDateTime start = LocalDateTime.of(2026, 4, 20, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 20, 11, 0);

        Booking cancelledFullHallBooking = createApprovedBooking(fullHall, user, start, end);
        cancelledFullHallBooking.cancel(user, "Cancelled");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(hallRepository.findById(requestedHall.getId())).thenReturn(Optional.of(requestedHall));
        when(bookingRepository.findByHallIdAndTimeRange(requestedHall.getId(), start, end)).thenReturn(List.of());
        when(bookingRepository.findByTimeRange(start, end)).thenReturn(List.of(cancelledFullHallBooking));
        when(blockedTimeRepository.findByHallIdAndTimeRange(requestedHall.getId(), start, end)).thenReturn(List.of());
        when(blockedTimeRepository.findAllByTimeRange(start, end)).thenReturn(List.of());
        when(bookingRequestRepository.save(any(BookingRequest.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0), UUID.randomUUID()));

        assertDoesNotThrow(() -> service.create(user.getId(), requestedHall.getId(), "Test", "desc", start, end));
    }

    @Test
    void create_request_rejects_conflict_with_blocked_time() {
        User user = createRepresentative();
        Hall hall = createPartHallA();
        LocalDateTime start = LocalDateTime.of(2026, 4, 20, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 20, 11, 0);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));
        when(bookingRepository.findByHallIdAndTimeRange(hall.getId(), start, end)).thenReturn(List.of());
        when(bookingRepository.findByTimeRange(start, end)).thenReturn(List.of());
        when(blockedTimeRepository.findByHallIdAndTimeRange(hall.getId(), start, end))
                .thenReturn(List.of(mock(de.hallenbelegung.application.domain.model.BlockedTime.class)));

        BookingConflictException exception = assertThrows(
                BookingConflictException.class,
                () -> service.create(user.getId(), hall.getId(), "Test", "desc", start, end)
        );

        assertEquals("Conflict with blocked time", exception.getMessage());
    }

    @Test
    void approve_success_creates_booking_and_marks_request_approved() {
        User admin = createAdmin();
        User requester = createRepresentative();
        Hall hall = createPartHallA();
        LocalDateTime start = LocalDateTime.of(2026, 4, 20, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 20, 11, 0);

        BookingRequest request = withId(
                BookingRequest.createNew("Volleyball", "Training", start, end, hall, requester),
                UUID.randomUUID()
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(bookingRepository.findByHallIdAndTimeRange(hall.getId(), start, end)).thenReturn(List.of());
        when(bookingRepository.findByTimeRange(start, end)).thenReturn(List.of());
        when(blockedTimeRepository.findByHallIdAndTimeRange(hall.getId(), start, end)).thenReturn(List.of());
        when(blockedTimeRepository.findAllByTimeRange(start, end)).thenReturn(List.of());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingRequestRepository.save(any(BookingRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.approve(admin.getId(), request.getId());

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());

        Booking createdBooking = bookingCaptor.getValue();
        assertEquals("Volleyball", createdBooking.getTitle());
        assertEquals(hall.getId(), createdBooking.getHall().getId());
        assertEquals(requester.getId(), createdBooking.getResponsibleUser().getId());
        assertEquals(admin.getId(), createdBooking.getCreatedBy().getId());

        assertTrue(request.isApproved());
        assertEquals(admin.getId(), request.getProcessedBy().getId());

        verify(notificationPort).notifyRequesterAboutBookingRequestApproved(eq(request), any(Booking.class));
    }

    @Test
    void approve_rejects_non_admin() {
        User representative = createRepresentative();

        when(userRepository.findById(representative.getId())).thenReturn(Optional.of(representative));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.approve(representative.getId(), UUID.randomUUID())
        );

        assertEquals("User not allowed to approve booking requests", exception.getMessage());
    }

    @Test
    void approve_rejects_non_open_request() {
        User admin = createAdmin();
        User requester = createRepresentative();
        Hall hall = createPartHallA();

        BookingRequest request = withId(
                BookingRequest.createNew(
                        "Volleyball",
                        "Training",
                        LocalDateTime.of(2026, 4, 20, 10, 0),
                        LocalDateTime.of(2026, 4, 20, 11, 0),
                        hall,
                        requester
                ),
                UUID.randomUUID()
        );
        request.approve(admin);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.approve(admin.getId(), request.getId())
        );

        assertEquals("Booking request is not open", exception.getMessage());
    }

    @Test
    void reject_success_marks_request_rejected_and_notifies_requester() {
        User admin = createAdmin();
        User requester = createRepresentative();
        Hall hall = createPartHallA();

        BookingRequest request = withId(
                BookingRequest.createNew(
                        "Volleyball",
                        "Training",
                        LocalDateTime.of(2026, 4, 20, 10, 0),
                        LocalDateTime.of(2026, 4, 20, 11, 0),
                        hall,
                        requester
                ),
                UUID.randomUUID()
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(bookingRequestRepository.save(any(BookingRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.reject(admin.getId(), request.getId(), "Halle bereits intern reserviert");

        assertTrue(request.isRejected());
        assertEquals("Halle bereits intern reserviert", request.getRejectionReason());
        assertEquals(admin.getId(), request.getProcessedBy().getId());

        verify(notificationPort).notifyRequesterAboutBookingRequestRejected(request, "Halle bereits intern reserviert");
    }

    @Test
    void getOpenRequests_returns_only_open_for_admin() {
        User admin = createAdmin();
        User requester = createRepresentative();
        Hall hall = createPartHallA();

        BookingRequest older = withId(
                BookingRequest.createNew(
                        "Alt",
                        "desc",
                        LocalDateTime.of(2026, 4, 21, 10, 0),
                        LocalDateTime.of(2026, 4, 21, 11, 0),
                        hall,
                        requester
                ),
                UUID.randomUUID()
        );

        BookingRequest newer = withId(
                BookingRequest.createNew(
                        "Neu",
                        "desc",
                        LocalDateTime.of(2026, 4, 22, 10, 0),
                        LocalDateTime.of(2026, 4, 22, 11, 0),
                        hall,
                        requester
                ),
                UUID.randomUUID()
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRequestRepository.findByStatus(BookingRequestStatus.PENDING))
                .thenReturn(List.of(older, newer));

        List<BookingRequest> result = service.getOpenRequests(admin.getId());

        assertEquals(2, result.size());
    }

    @Test
    void getRequestsByUser_returns_only_own_requests() {
        User requester = createRepresentative();
        Hall hall = createPartHallA();

        BookingRequest request1 = withId(
                BookingRequest.createNew(
                        "Training 1",
                        "desc",
                        LocalDateTime.of(2026, 4, 21, 10, 0),
                        LocalDateTime.of(2026, 4, 21, 11, 0),
                        hall,
                        requester
                ),
                UUID.randomUUID()
        );

        BookingRequest request2 = withId(
                BookingRequest.createNew(
                        "Training 2",
                        "desc",
                        LocalDateTime.of(2026, 4, 22, 10, 0),
                        LocalDateTime.of(2026, 4, 22, 11, 0),
                        hall,
                        requester
                ),
                UUID.randomUUID()
        );

        when(userRepository.findById(requester.getId())).thenReturn(Optional.of(requester));
        when(bookingRequestRepository.findByRequestedByUserId(requester.getId()))
                .thenReturn(List.of(request1, request2));

        List<BookingRequest> result = service.getRequestsByUser(requester.getId());

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(r -> r.getRequestedBy().getId().equals(requester.getId())));
    }

    @Test
    void getById_allows_admin() {
        User admin = createAdmin();
        User requester = createRepresentative();
        Hall hall = createPartHallA();

        BookingRequest request = withId(
                BookingRequest.createNew(
                        "Training",
                        "desc",
                        LocalDateTime.of(2026, 4, 21, 10, 0),
                        LocalDateTime.of(2026, 4, 21, 11, 0),
                        hall,
                        requester
                ),
                UUID.randomUUID()
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        BookingRequest result = service.getById(admin.getId(), request.getId());

        assertEquals(request.getId(), result.getId());
    }

    @Test
    void getById_allows_owner() {
        User requester = createRepresentative();
        Hall hall = createPartHallA();

        BookingRequest request = withId(
                BookingRequest.createNew(
                        "Training",
                        "desc",
                        LocalDateTime.of(2026, 4, 21, 10, 0),
                        LocalDateTime.of(2026, 4, 21, 11, 0),
                        hall,
                        requester
                ),
                UUID.randomUUID()
        );

        when(userRepository.findById(requester.getId())).thenReturn(Optional.of(requester));
        when(bookingRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        BookingRequest result = service.getById(requester.getId(), request.getId());

        assertEquals(request.getId(), result.getId());
    }

    @Test
    void getById_rejects_other_representative() {
        User owner = createRepresentative();
        User otherUser = new User(
                UUID.randomUUID(),
                "Other",
                "Rep",
                "other@example.com",
                "hash",
                Role.CLUB_REPRESENTATIVE,
                true,
                Instant.now(),
                Instant.now()
        );
        Hall hall = createPartHallA();

        BookingRequest request = withId(
                BookingRequest.createNew(
                        "Training",
                        "desc",
                        LocalDateTime.of(2026, 4, 21, 10, 0),
                        LocalDateTime.of(2026, 4, 21, 11, 0),
                        hall,
                        owner
                ),
                UUID.randomUUID()
        );

        when(userRepository.findById(otherUser.getId())).thenReturn(Optional.of(otherUser));
        when(bookingRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.getById(otherUser.getId(), request.getId())
        );

        assertEquals("User not allowed to view this booking request", exception.getMessage());
    }

    @Test
    void approve_rejects_inactive_hall() {
        User admin = createAdmin();
        User requester = createRepresentative();
        Hall inactiveHall = createInactiveHall();

        BookingRequest request = withId(
                BookingRequest.createNew(
                        "Training",
                        "desc",
                        LocalDateTime.of(2026, 4, 21, 10, 0),
                        LocalDateTime.of(2026, 4, 21, 11, 0),
                        inactiveHall,
                        requester
                ),
                UUID.randomUUID()
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.approve(admin.getId(), request.getId())
        );

        assertEquals("Hall inactive", exception.getMessage());
    }
}