package de.hallenbelegung.application.domain.service;

import de.hallenbelegung.application.domain.exception.ForbiddenException;
import de.hallenbelegung.application.domain.exception.NotFoundException;
import de.hallenbelegung.application.domain.exception.ValidationException;
import de.hallenbelegung.application.domain.model.Booking;
import de.hallenbelegung.application.domain.model.BookingStatus;
import de.hallenbelegung.application.domain.model.Hall;
import de.hallenbelegung.application.domain.model.HallType;
import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.out.BlockedTimeRepositoryPort;
import de.hallenbelegung.application.domain.port.out.BookingRepositoryPort;
import de.hallenbelegung.application.domain.port.out.HallConfigPort;
import de.hallenbelegung.application.domain.port.out.HallRepositoryPort;
import de.hallenbelegung.application.domain.port.out.NotificationPort;
import de.hallenbelegung.application.domain.port.out.UserRepositoryPort;
import de.hallenbelegung.application.domain.view.BookingDetailView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

public class BookingServiceTest {

    private BookingRepositoryPort bookingRepository;
    private UserRepositoryPort userRepository;
    private NotificationPort notificationPort;
    private HallRepositoryPort hallRepository;
    private BlockedTimeRepositoryPort blockedTimeRepository;
    private HallConfigPort config;
    private Clock clock;

    private BookingService service;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepositoryPort.class);
        userRepository = mock(UserRepositoryPort.class);
        notificationPort = mock(NotificationPort.class);
        hallRepository = mock(HallRepositoryPort.class);
        blockedTimeRepository = mock(BlockedTimeRepositoryPort.class);
        config = mock(HallConfigPort.class);

        clock = Clock.fixed(
                Instant.parse("2026-04-14T10:00:00Z"),
                ZoneId.of("Europe/Berlin")
        );

        when(config.bookingIntervalMinutes()).thenReturn(15);
        when(config.openingStart()).thenReturn(LocalTime.of(8, 0));
        when(config.openingEnd()).thenReturn(LocalTime.of(22, 0));

        service = new BookingService(
                bookingRepository,
                userRepository,
                notificationPort,
                hallRepository,
                blockedTimeRepository,
                config,
                clock
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

    private User createOtherRepresentative() {
        return new User(
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

    private Hall createPartHallB() {
        return new Hall(
                UUID.randomUUID(),
                "Halle B",
                "Teilhalle B",
                true,
                Instant.now(),
                Instant.now(),
                HallType.PART_LARGE
        );
    }

    private Hall createFullHall() {
        return new Hall(
                UUID.randomUUID(),
                "Gesamthalle",
                "Gesamte Halle",
                true,
                Instant.now(),
                Instant.now(),
                HallType.FULL
        );
    }

    private Hall createInactiveHall() {
        return new Hall(
                UUID.randomUUID(),
                "Inaktive Halle",
                "desc",
                false,
                Instant.now(),
                Instant.now(),
                HallType.PART_SMALL
        );
    }

    private Booking createBooking(User owner, Hall hall, LocalDateTime start, LocalDateTime end) {
        return new Booking(
                UUID.randomUUID(),
                "Volleyball Jugend",
                "Training",
                start,
                end,
                BookingStatus.APPROVED,
                15,
                true,
                "Kommentar",
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

    private Booking createBookingWithId(UUID bookingId, User owner, Hall hall, LocalDateTime start, LocalDateTime end) {
        return new Booking(
                bookingId,
                "Volleyball Jugend",
                "Training",
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
    void getById_guest_can_view_public_details_but_not_feedback_or_actions() {
        User owner = createRepresentative();
        Hall hall = createPartHallA();
        Booking booking = createBooking(
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        BookingDetailView result = service.getById(null, booking.getId());

        assertEquals(booking.getId(), result.id());
        assertEquals("Volleyball Jugend", result.title());
        assertEquals("Halle A", result.hallName());
        assertEquals(owner.getFullName(), result.responsibleUserName());
        assertNull(result.participantCount());
        assertNull(result.feedbackComment());
        assertFalse(result.canViewFeedback());
        assertFalse(result.canEdit());
        assertFalse(result.canCancel());
    }

    @Test
    void getById_owner_can_view_feedback_and_cancel() {
        User owner = createRepresentative();
        Hall hall = createPartHallA();
        Booking booking = createBooking(
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        BookingDetailView result = service.getById(owner.getId(), booking.getId());

        assertEquals(15, result.participantCount());
        assertEquals("Kommentar", result.feedbackComment());
        assertTrue(result.canViewFeedback());
        assertFalse(result.canEdit());
        assertTrue(result.canCancel());
    }

    @Test
    void getById_admin_can_view_feedback_edit_and_cancel() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createPartHallA();
        Booking booking = createBooking(
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        BookingDetailView result = service.getById(admin.getId(), booking.getId());

        assertTrue(result.canViewFeedback());
        assertTrue(result.canEdit());
        assertTrue(result.canCancel());
    }

    @Test
    void getById_other_user_gets_limited_public_view() {
        User owner = createRepresentative();
        User other = createOtherRepresentative();
        Hall hall = createPartHallA();
        Booking booking = createBooking(
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        when(userRepository.findById(other.getId())).thenReturn(Optional.of(other));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        BookingDetailView result = service.getById(other.getId(), booking.getId());

        assertNull(result.participantCount());
        assertNull(result.feedbackComment());
        assertFalse(result.canViewFeedback());
        assertFalse(result.canEdit());
        assertFalse(result.canCancel());
    }

    @Test
    void getById_rejects_unknown_booking() {
        User owner = createRepresentative();

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingRepository.findById(any())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.getById(owner.getId(), UUID.randomUUID())
        );

        assertEquals("Booking not found", exception.getMessage());
    }

    @Test
    void getById_rejects_unknown_user() {
        UUID unknownUserId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        Booking booking = createBooking(
                createRepresentative(),
                createPartHallA(),
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(userRepository.findById(unknownUserId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.getById(unknownUserId, bookingId)
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void getById_rejects_inactive_user() {
        User inactive = createInactiveRepresentative();
        Booking booking = createBooking(
                createRepresentative(),
                createPartHallA(),
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(userRepository.findById(inactive.getId())).thenReturn(Optional.of(inactive));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.getById(inactive.getId(), booking.getId())
        );

        assertEquals("User inactive", exception.getMessage());
    }

    @Test
    void getBookingsByUser_returns_sorted_descending_by_start_time() {
        User owner = createRepresentative();
        Hall hall = createPartHallA();

        Booking older = createBooking(
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );
        Booking newer = createBooking(
                owner,
                hall,
                LocalDateTime.of(2026, 4, 22, 10, 0),
                LocalDateTime.of(2026, 4, 22, 11, 0)
        );

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingRepository.findByResponsibleUserId(owner.getId())).thenReturn(List.of(older, newer));

        List<Booking> result = service.getBookingsByUser(owner.getId());

        assertEquals(2, result.size());
        assertEquals(newer.getStartAt(), result.get(0).getStartAt());
        assertEquals(older.getStartAt(), result.get(1).getStartAt());
    }

    @Test
    void getBookingsByUser_rejects_unknown_user() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.getBookingsByUser(userId)
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void getBookingsByUser_rejects_inactive_user() {
        User inactive = createInactiveRepresentative();

        when(userRepository.findById(inactive.getId())).thenReturn(Optional.of(inactive));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.getBookingsByUser(inactive.getId())
        );

        assertEquals("User inactive", exception.getMessage());
    }

    @Test
    void cancel_success_for_owner() {
        User owner = createRepresentative();
        Hall hall = createPartHallA();
        Booking booking = createBooking(
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        service.cancel(owner.getId(), booking.getId(), "Krankheit");

        assertTrue(booking.isCancelled());
        assertEquals("Krankheit", booking.getCancelReason());
        assertEquals(owner.getId(), booking.getCancelledBy().getId());
        verify(bookingRepository).save(booking);
        verify(notificationPort, never()).notifyRequesterAboutBookingCancelledByAdmin(any(), any());
    }

    @Test
    void cancel_success_for_admin_and_notifies_requester() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createPartHallA();
        Booking booking = createBooking(
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        service.cancel(admin.getId(), booking.getId(), "Gemeindeveranstaltung");

        assertTrue(booking.isCancelled());
        assertEquals("Gemeindeveranstaltung", booking.getCancelReason());
        assertEquals(admin.getId(), booking.getCancelledBy().getId());

        verify(notificationPort).notifyRequesterAboutBookingCancelledByAdmin(booking, "Gemeindeveranstaltung");
    }

    @Test
    void cancel_accepts_null_reason() {
        User owner = createRepresentative();
        Hall hall = createPartHallA();
        Booking booking = createBooking(
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        service.cancel(owner.getId(), booking.getId(), null);

        assertTrue(booking.isCancelled());
        assertNull(booking.getCancelReason());
    }

    @Test
    void cancel_rejects_unknown_user() {
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.cancel(userId, bookingId, "Reason")
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void cancel_rejects_inactive_user() {
        User inactive = createInactiveRepresentative();
        Booking booking = createBooking(
                createRepresentative(),
                createPartHallA(),
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        when(userRepository.findById(inactive.getId())).thenReturn(Optional.of(inactive));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.cancel(inactive.getId(), booking.getId(), "Reason")
        );

        assertEquals("User inactive", exception.getMessage());
    }

    @Test
    void cancel_rejects_unknown_booking() {
        User owner = createRepresentative();

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingRepository.findById(any())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.cancel(owner.getId(), UUID.randomUUID(), "Reason")
        );

        assertEquals("Booking not found", exception.getMessage());
    }

    @Test
    void cancel_rejects_already_cancelled_booking() {
        User owner = createRepresentative();
        Hall hall = createPartHallA();
        Booking booking = createBooking(
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );
        booking.cancel(owner, "Schon storniert");

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.cancel(owner.getId(), booking.getId(), "Nochmal")
        );

        assertEquals("Booking is already cancelled", exception.getMessage());
    }

    @Test
    void cancel_rejects_already_cancelled_booking_before_permission_checks() {
        User owner = createRepresentative();
        User other = createOtherRepresentative();
        Hall hall = createPartHallA();
        Booking booking = createBooking(
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );
        booking.cancel(owner, "Schon storniert");

        when(userRepository.findById(other.getId())).thenReturn(Optional.of(other));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.cancel(other.getId(), booking.getId(), "Nochmal")
        );

        assertEquals("Booking is already cancelled", exception.getMessage());
    }

    @Test
    void cancel_rejects_other_user() {
        User owner = createRepresentative();
        User other = createOtherRepresentative();
        Hall hall = createPartHallA();
        Booking booking = createBooking(
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        when(userRepository.findById(other.getId())).thenReturn(Optional.of(other));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.cancel(other.getId(), booking.getId(), "Nope")
        );

        assertEquals("User not allowed to cancel this booking", exception.getMessage());
    }

    @Test
    void addFeedback_success_for_owner() {
        User owner = createRepresentative();
        Hall hall = createPartHallA();
        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 13, 10, 0),
                LocalDateTime.of(2026, 4, 13, 11, 0)
        );

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        service.addFeedback(owner.getId(), booking.getId(), 18, "Gutes Training");

        assertEquals(18, booking.getParticipantCount());
        assertEquals("Gutes Training", booking.getFeedbackComment());
        assertTrue(booking.isConducted());
        verify(bookingRepository).save(booking);
    }

    @Test
    void addFeedback_success_for_admin() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createPartHallA();
        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 13, 10, 0),
                LocalDateTime.of(2026, 4, 13, 11, 0)
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        service.addFeedback(admin.getId(), booking.getId(), 20, "Admin Feedback");

        assertEquals(20, booking.getParticipantCount());
        assertEquals("Admin Feedback", booking.getFeedbackComment());
        assertTrue(booking.isConducted());
    }

    @Test
    void addFeedback_allows_null_participant_count() {
        User owner = createRepresentative();
        Hall hall = createPartHallA();
        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 13, 10, 0),
                LocalDateTime.of(2026, 4, 13, 11, 0)
        );

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        service.addFeedback(owner.getId(), booking.getId(), null, "Nur Kommentar");

        assertNull(booking.getParticipantCount());
        assertEquals("Nur Kommentar", booking.getFeedbackComment());
        assertTrue(booking.isConducted());
    }

    @Test
    void updateFeedback_delegates_to_addFeedback() {
        User owner = createRepresentative();
        Hall hall = createPartHallA();
        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 13, 10, 0),
                LocalDateTime.of(2026, 4, 13, 11, 0)
        );

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        service.updateFeedback(booking.getId(), 9, "Update Kommentar", owner.getId());

        assertEquals(9, booking.getParticipantCount());
        assertEquals("Update Kommentar", booking.getFeedbackComment());
        assertTrue(booking.isConducted());
    }

    @Test
    void addFeedback_rejects_unknown_user() {
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.addFeedback(userId, bookingId, 10, "Kommentar")
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void addFeedback_rejects_inactive_user() {
        User inactive = createInactiveRepresentative();

        when(userRepository.findById(inactive.getId())).thenReturn(Optional.of(inactive));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.addFeedback(inactive.getId(), UUID.randomUUID(), 10, "Kommentar")
        );

        assertEquals("User inactive", exception.getMessage());
    }

    @Test
    void addFeedback_rejects_unknown_booking() {
        User owner = createRepresentative();

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingRepository.findById(any())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.addFeedback(owner.getId(), UUID.randomUUID(), 10, "Kommentar")
        );

        assertEquals("Booking not found", exception.getMessage());
    }

    @Test
    void addFeedback_rejects_cancelled_booking() {
        User owner = createRepresentative();
        Hall hall = createPartHallA();
        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 13, 10, 0),
                LocalDateTime.of(2026, 4, 13, 11, 0)
        );
        booking.cancel(owner, "Ausfall");

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.addFeedback(owner.getId(), booking.getId(), 12, "Kommentar")
        );

        assertEquals("Cannot add feedback to cancelled booking", exception.getMessage());
    }

    @Test
    void addFeedback_rejects_cancelled_booking_before_permission_checks() {
        User owner = createRepresentative();
        User other = createOtherRepresentative();
        Hall hall = createPartHallA();
        Booking booking = createBooking(
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );
        booking.cancel(owner, "Ausfall");

        when(userRepository.findById(other.getId())).thenReturn(Optional.of(other));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.addFeedback(other.getId(), booking.getId(), 12, "Kommentar")
        );

        assertEquals("Cannot add feedback to cancelled booking", exception.getMessage());
    }

    @Test
    void addFeedback_rejects_other_user() {
        User owner = createRepresentative();
        User other = createOtherRepresentative();
        Hall hall = createPartHallA();
        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 13, 10, 0),
                LocalDateTime.of(2026, 4, 13, 11, 0)
        );

        when(userRepository.findById(other.getId())).thenReturn(Optional.of(other));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.addFeedback(other.getId(), booking.getId(), 10, "Kommentar")
        );

        assertEquals("User not allowed to add feedback to this booking", exception.getMessage());
    }

    @Test
    void addFeedback_rejects_negative_participant_count() {
        User owner = createRepresentative();
        Hall hall = createPartHallA();
        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 13, 10, 0),
                LocalDateTime.of(2026, 4, 13, 11, 0)
        );

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.addFeedback(owner.getId(), booking.getId(), -1, "Kommentar")
        );

        assertEquals("Participant count must not be negative", exception.getMessage());
    }

    @Test
    void update_success_for_admin_same_part_hall_no_conflicts() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall oldHall = createPartHallA();
        Hall newHall = createPartHallB();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                oldHall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        LocalDateTime newStart = LocalDateTime.of(2026, 4, 21, 12, 0);
        LocalDateTime newEnd = LocalDateTime.of(2026, 4, 21, 13, 0);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(hallRepository.findById(newHall.getId())).thenReturn(Optional.of(newHall));
        when(bookingRepository.findByHallIdAndTimeRange(newHall.getId(), newStart, newEnd)).thenReturn(List.of());
        when(bookingRepository.findByTimeRange(newStart, newEnd)).thenReturn(List.of());
        when(blockedTimeRepository.findByHallIdAndTimeRange(newHall.getId(), newStart, newEnd)).thenReturn(List.of());
        when(blockedTimeRepository.findAllByTimeRange(newStart, newEnd)).thenReturn(List.of());
        when(bookingRepository.save(booking)).thenReturn(booking);

        Booking result = service.update(
                admin.getId(),
                booking.getId(),
                newHall.getId(),
                "Neuer Titel",
                "Neue Beschreibung",
                newStart,
                newEnd
        );

        assertEquals("Neuer Titel", result.getTitle());
        assertEquals("Neue Beschreibung", result.getDescription());
        assertEquals(newStart, result.getStartAt());
        assertEquals(newEnd, result.getEndAt());
        assertEquals(newHall.getId(), result.getHall().getId());

        verify(notificationPort).notifyRequesterAboutBookingUpdated(result);
    }

    @Test
    void update_success_for_admin_full_hall_when_no_conflicts() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall oldHall = createPartHallA();
        Hall fullHall = createFullHall();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                oldHall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        LocalDateTime newStart = LocalDateTime.of(2026, 4, 21, 18, 0);
        LocalDateTime newEnd = LocalDateTime.of(2026, 4, 21, 19, 0);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(hallRepository.findById(fullHall.getId())).thenReturn(Optional.of(fullHall));
        when(bookingRepository.findByTimeRange(newStart, newEnd)).thenReturn(List.of());
        when(blockedTimeRepository.findAllByTimeRange(newStart, newEnd)).thenReturn(List.of());
        when(bookingRepository.save(booking)).thenReturn(booking);

        Booking result = service.update(
                admin.getId(),
                booking.getId(),
                fullHall.getId(),
                "Gesamthalle",
                "Event",
                newStart,
                newEnd
        );

        assertEquals(fullHall.getId(), result.getHall().getId());
        verify(notificationPort).notifyRequesterAboutBookingUpdated(result);
    }

    @Test
    void update_ignores_current_booking_in_conflict_checks() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createPartHallA();

        UUID bookingId = UUID.randomUUID();
        Booking booking = createBookingWithId(
                bookingId,
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        Booking sameBookingReturnedByRepo = createBookingWithId(
                bookingId,
                owner,
                hall,
                LocalDateTime.of(2026, 4, 21, 10, 0),
                LocalDateTime.of(2026, 4, 21, 11, 0)
        );

        LocalDateTime newStart = LocalDateTime.of(2026, 4, 21, 10, 0);
        LocalDateTime newEnd = LocalDateTime.of(2026, 4, 21, 11, 0);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));
        when(bookingRepository.findByHallIdAndTimeRange(hall.getId(), newStart, newEnd))
                .thenReturn(List.of(sameBookingReturnedByRepo));
        when(bookingRepository.findByTimeRange(newStart, newEnd)).thenReturn(List.of(sameBookingReturnedByRepo));
        when(blockedTimeRepository.findByHallIdAndTimeRange(hall.getId(), newStart, newEnd)).thenReturn(List.of());
        when(blockedTimeRepository.findAllByTimeRange(newStart, newEnd)).thenReturn(List.of());
        when(bookingRepository.save(booking)).thenReturn(booking);

        Booking result = service.update(
                admin.getId(),
                booking.getId(),
                hall.getId(),
                "Titel",
                "Beschreibung",
                newStart,
                newEnd
        );

        assertEquals("Titel", result.getTitle());
    }

    @Test
    void update_rejects_unknown_user() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.update(
                        userId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Titel",
                        "Beschreibung",
                        LocalDateTime.of(2026, 4, 21, 10, 0),
                        LocalDateTime.of(2026, 4, 21, 11, 0)
                )
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void update_rejects_inactive_user() {
        User inactive = createInactiveRepresentative();

        when(userRepository.findById(inactive.getId())).thenReturn(Optional.of(inactive));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.update(
                        inactive.getId(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Titel",
                        "Beschreibung",
                        LocalDateTime.of(2026, 4, 21, 10, 0),
                        LocalDateTime.of(2026, 4, 21, 11, 0)
                )
        );

        assertEquals("User inactive", exception.getMessage());
    }

    @Test
    void update_rejects_non_admin() {
        User owner = createRepresentative();

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.update(
                        owner.getId(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Titel",
                        "Beschreibung",
                        LocalDateTime.of(2026, 4, 21, 10, 0),
                        LocalDateTime.of(2026, 4, 21, 11, 0)
                )
        );

        assertEquals("User not allowed to update bookings", exception.getMessage());
    }

    @Test
    void update_rejects_unknown_booking() {
        User admin = createAdmin();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(any())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.update(
                        admin.getId(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Titel",
                        "Beschreibung",
                        LocalDateTime.of(2026, 4, 21, 10, 0),
                        LocalDateTime.of(2026, 4, 21, 11, 0)
                )
        );

        assertEquals("Booking not found", exception.getMessage());
    }

    @Test
    void update_rejects_cancelled_booking() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createPartHallA();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );
        booking.cancel(admin, "Storniert");

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.update(
                        admin.getId(),
                        booking.getId(),
                        hall.getId(),
                        "Titel",
                        "Beschreibung",
                        LocalDateTime.of(2026, 4, 21, 10, 0),
                        LocalDateTime.of(2026, 4, 21, 11, 0)
                )
        );

        assertEquals("Cancelled booking cannot be updated", exception.getMessage());
    }

    @Test
    void update_rejects_unknown_hall() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createPartHallA();
        UUID unknownHallId = UUID.randomUUID();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(hallRepository.findById(unknownHallId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.update(
                        admin.getId(),
                        booking.getId(),
                        unknownHallId,
                        "Titel",
                        "Beschreibung",
                        LocalDateTime.of(2026, 4, 21, 10, 0),
                        LocalDateTime.of(2026, 4, 21, 11, 0)
                )
        );

        assertEquals("Hall not found", exception.getMessage());
    }

    @Test
    void update_rejects_inactive_hall() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall currentHall = createPartHallA();
        Hall inactiveHall = createInactiveHall();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                currentHall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(hallRepository.findById(inactiveHall.getId())).thenReturn(Optional.of(inactiveHall));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.update(
                        admin.getId(),
                        booking.getId(),
                        inactiveHall.getId(),
                        "Titel",
                        "Beschreibung",
                        LocalDateTime.of(2026, 4, 21, 10, 0),
                        LocalDateTime.of(2026, 4, 21, 11, 0)
                )
        );

        assertEquals("Hall inactive", exception.getMessage());
    }

    @Test
    void update_rejects_blank_title() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createPartHallA();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.update(
                        admin.getId(),
                        booking.getId(),
                        hall.getId(),
                        "   ",
                        "Beschreibung",
                        LocalDateTime.of(2026, 4, 21, 10, 0),
                        LocalDateTime.of(2026, 4, 21, 11, 0)
                )
        );

        assertEquals("Title required", exception.getMessage());
    }

    @Test
    void update_rejects_null_title() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createPartHallA();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.update(
                        admin.getId(),
                        booking.getId(),
                        hall.getId(),
                        null,
                        "Beschreibung",
                        LocalDateTime.of(2026, 4, 21, 10, 0),
                        LocalDateTime.of(2026, 4, 21, 11, 0)
                )
        );

        assertEquals("Title required", exception.getMessage());
    }

    @Test
    void update_rejects_null_times() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createPartHallA();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.update(
                        admin.getId(),
                        booking.getId(),
                        hall.getId(),
                        "Titel",
                        "Beschreibung",
                        null,
                        LocalDateTime.of(2026, 4, 21, 11, 0)
                )
        );

        assertEquals("Start time and end time are required", exception.getMessage());
    }

    @Test
    void update_rejects_start_after_end() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createPartHallA();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.update(
                        admin.getId(),
                        booking.getId(),
                        hall.getId(),
                        "Titel",
                        "Beschreibung",
                        LocalDateTime.of(2026, 4, 21, 12, 0),
                        LocalDateTime.of(2026, 4, 21, 11, 0)
                )
        );

        assertEquals("Start must be before end", exception.getMessage());
    }

    @Test
    void update_rejects_equal_start_and_end() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createPartHallA();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        LocalDateTime same = LocalDateTime.of(2026, 4, 21, 10, 0);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.update(
                        admin.getId(),
                        booking.getId(),
                        hall.getId(),
                        "Titel",
                        "Beschreibung",
                        same,
                        same
                )
        );

        assertEquals("Start must be before end", exception.getMessage());
    }

    @Test
    void update_rejects_past_booking() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createPartHallA();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.update(
                        admin.getId(),
                        booking.getId(),
                        hall.getId(),
                        "Titel",
                        "Beschreibung",
                        LocalDateTime.of(2026, 4, 13, 10, 0),
                        LocalDateTime.of(2026, 4, 13, 11, 0)
                )
        );

        assertEquals("Cannot move booking into the past", exception.getMessage());
    }

    @Test
    void update_rejects_more_than_one_year_in_future() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createPartHallA();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.update(
                        admin.getId(),
                        booking.getId(),
                        hall.getId(),
                        "Titel",
                        "Beschreibung",
                        LocalDateTime.of(2027, 4, 15, 10, 0),
                        LocalDateTime.of(2027, 4, 15, 11, 0)
                )
        );

        assertEquals("Booking must not be more than one year in advance", exception.getMessage());
    }

    @Test
    void update_rejects_non_15_minute_grid() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createPartHallA();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.update(
                        admin.getId(),
                        booking.getId(),
                        hall.getId(),
                        "Titel",
                        "Beschreibung",
                        LocalDateTime.of(2026, 4, 21, 10, 10),
                        LocalDateTime.of(2026, 4, 21, 11, 0)
                )
        );

        assertEquals("Not on valid time grid", exception.getMessage());
    }

    @Test
    void update_rejects_seconds_and_nanoseconds() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createPartHallA();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        LocalDateTime start = LocalDateTime.of(2026, 4, 21, 10, 15, 1);
        LocalDateTime end = LocalDateTime.of(2026, 4, 21, 11, 0);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.update(
                        admin.getId(),
                        booking.getId(),
                        hall.getId(),
                        "Titel",
                        "Beschreibung",
                        start,
                        end
                )
        );

        assertEquals("Seconds and nanoseconds are not allowed", exception.getMessage());
    }

    @Test
    void update_rejects_seconds_and_nanoseconds_in_end_time() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createPartHallA();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        LocalDateTime start = LocalDateTime.of(2026, 4, 21, 10, 15);
        LocalDateTime end = LocalDateTime.of(2026, 4, 21, 11, 0, 1);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.update(
                        admin.getId(),
                        booking.getId(),
                        hall.getId(),
                        "Titel",
                        "Beschreibung",
                        start,
                        end
                )
        );

        assertEquals("Seconds and nanoseconds are not allowed", exception.getMessage());
    }

    @Test
    void update_accepts_exact_opening_boundaries() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createPartHallA();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        LocalDateTime start = LocalDateTime.of(2026, 4, 21, 8, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 21, 22, 0);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));
        when(bookingRepository.findByHallIdAndTimeRange(hall.getId(), start, end)).thenReturn(List.of());
        when(bookingRepository.findByTimeRange(start, end)).thenReturn(List.of());
        when(blockedTimeRepository.findByHallIdAndTimeRange(hall.getId(), start, end)).thenReturn(List.of());
        when(blockedTimeRepository.findAllByTimeRange(start, end)).thenReturn(List.of());
        when(bookingRepository.save(booking)).thenReturn(booking);

        Booking result = service.update(admin.getId(), booking.getId(), hall.getId(), "Titel", "Beschreibung", start, end);

        assertEquals(start, result.getStartAt());
        assertEquals(end, result.getEndAt());
    }

    @Test
    void update_accepts_exact_one_year_boundary() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createPartHallA();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        LocalDateTime start = LocalDateTime.of(2027, 4, 14, 10, 0);
        LocalDateTime end = LocalDateTime.of(2027, 4, 14, 11, 0);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));
        when(bookingRepository.findByHallIdAndTimeRange(hall.getId(), start, end)).thenReturn(List.of());
        when(bookingRepository.findByTimeRange(start, end)).thenReturn(List.of());
        when(blockedTimeRepository.findByHallIdAndTimeRange(hall.getId(), start, end)).thenReturn(List.of());
        when(blockedTimeRepository.findAllByTimeRange(start, end)).thenReturn(List.of());
        when(bookingRepository.save(booking)).thenReturn(booking);

        Booking result = service.update(admin.getId(), booking.getId(), hall.getId(), "Titel", "Beschreibung", start, end);

        assertEquals(start, result.getStartAt());
    }

    @Test
    void update_rejects_outside_opening_hours() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createPartHallA();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.update(
                        admin.getId(),
                        booking.getId(),
                        hall.getId(),
                        "Titel",
                        "Beschreibung",
                        LocalDateTime.of(2026, 4, 21, 7, 45),
                        LocalDateTime.of(2026, 4, 21, 9, 0)
                )
        );

        assertEquals("Outside opening hours", exception.getMessage());
    }

    @Test
    void update_rejects_conflict_with_existing_booking_same_part_hall() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createPartHallA();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );
        Booking conflicting = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 21, 10, 0),
                LocalDateTime.of(2026, 4, 21, 11, 0)
        );

        LocalDateTime newStart = LocalDateTime.of(2026, 4, 21, 10, 0);
        LocalDateTime newEnd = LocalDateTime.of(2026, 4, 21, 11, 0);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));
        when(bookingRepository.findByHallIdAndTimeRange(hall.getId(), newStart, newEnd))
                .thenReturn(List.of(conflicting));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.update(
                        admin.getId(),
                        booking.getId(),
                        hall.getId(),
                        "Titel",
                        "Beschreibung",
                        newStart,
                        newEnd
                )
        );

        assertEquals("Conflict with existing booking", exception.getMessage());
    }

    @Test
    void update_rejects_conflict_with_full_hall_booking_when_moving_to_part_hall() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall partHall = createPartHallA();
        Hall fullHall = createFullHall();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                partHall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );
        Booking fullHallBooking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                fullHall,
                LocalDateTime.of(2026, 4, 21, 10, 0),
                LocalDateTime.of(2026, 4, 21, 11, 0)
        );

        LocalDateTime newStart = LocalDateTime.of(2026, 4, 21, 10, 0);
        LocalDateTime newEnd = LocalDateTime.of(2026, 4, 21, 11, 0);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(hallRepository.findById(partHall.getId())).thenReturn(Optional.of(partHall));
        when(bookingRepository.findByHallIdAndTimeRange(partHall.getId(), newStart, newEnd)).thenReturn(List.of());
        when(bookingRepository.findByTimeRange(newStart, newEnd)).thenReturn(List.of(fullHallBooking));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.update(
                        admin.getId(),
                        booking.getId(),
                        partHall.getId(),
                        "Titel",
                        "Beschreibung",
                        newStart,
                        newEnd
                )
        );

        assertEquals("Conflict with full hall booking", exception.getMessage());
    }

    @Test
    void update_rejects_conflict_with_blocked_time_same_part_hall() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createPartHallA();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        LocalDateTime newStart = LocalDateTime.of(2026, 4, 21, 10, 0);
        LocalDateTime newEnd = LocalDateTime.of(2026, 4, 21, 11, 0);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));
        when(bookingRepository.findByHallIdAndTimeRange(hall.getId(), newStart, newEnd)).thenReturn(List.of());
        when(bookingRepository.findByTimeRange(newStart, newEnd)).thenReturn(List.of());
        when(blockedTimeRepository.findByHallIdAndTimeRange(hall.getId(), newStart, newEnd))
                .thenReturn(List.of(mock(de.hallenbelegung.application.domain.model.BlockedTime.class)));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.update(
                        admin.getId(),
                        booking.getId(),
                        hall.getId(),
                        "Titel",
                        "Beschreibung",
                        newStart,
                        newEnd
                )
        );

        assertEquals("Conflict with blocked time", exception.getMessage());
    }

    @Test
    void update_rejects_conflict_with_full_hall_blocked_time_when_moving_to_part_hall() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createPartHallA();
        Hall fullHall = createFullHall();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        de.hallenbelegung.application.domain.model.BlockedTime fullHallBlockedTime =
                mock(de.hallenbelegung.application.domain.model.BlockedTime.class);
        when(fullHallBlockedTime.getHall()).thenReturn(fullHall);

        LocalDateTime newStart = LocalDateTime.of(2026, 4, 21, 10, 0);
        LocalDateTime newEnd = LocalDateTime.of(2026, 4, 21, 11, 0);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));
        when(bookingRepository.findByTimeRange(newStart, newEnd)).thenReturn(List.of());
        when(blockedTimeRepository.findAllByTimeRange(newStart, newEnd)).thenReturn(List.of(fullHallBlockedTime));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.update(
                        admin.getId(),
                        booking.getId(),
                        hall.getId(),
                        "Titel",
                        "Beschreibung",
                        newStart,
                        newEnd
                )
        );

        assertEquals("Conflict with full hall blocked time", exception.getMessage());
    }

    @Test
    void update_rejects_conflict_when_moving_to_full_hall_and_any_active_booking_exists() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall currentHall = createPartHallA();
        Hall fullHall = createFullHall();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                currentHall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );
        Booking anotherActiveBooking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                createPartHallB(),
                LocalDateTime.of(2026, 4, 21, 18, 0),
                LocalDateTime.of(2026, 4, 21, 19, 0)
        );

        LocalDateTime newStart = LocalDateTime.of(2026, 4, 21, 18, 0);
        LocalDateTime newEnd = LocalDateTime.of(2026, 4, 21, 19, 0);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(hallRepository.findById(fullHall.getId())).thenReturn(Optional.of(fullHall));
        when(bookingRepository.findByTimeRange(newStart, newEnd)).thenReturn(List.of(anotherActiveBooking));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.update(
                        admin.getId(),
                        booking.getId(),
                        fullHall.getId(),
                        "Titel",
                        "Beschreibung",
                        newStart,
                        newEnd
                )
        );

        assertEquals("Conflict with existing booking", exception.getMessage());
    }

    @Test
    void update_rejects_conflict_when_moving_to_full_hall_and_blocked_time_exists() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall currentHall = createPartHallA();
        Hall fullHall = createFullHall();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                currentHall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        LocalDateTime newStart = LocalDateTime.of(2026, 4, 21, 18, 0);
        LocalDateTime newEnd = LocalDateTime.of(2026, 4, 21, 19, 0);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(hallRepository.findById(fullHall.getId())).thenReturn(Optional.of(fullHall));
        when(bookingRepository.findByTimeRange(newStart, newEnd)).thenReturn(List.of());
        when(blockedTimeRepository.findAllByTimeRange(newStart, newEnd))
                .thenReturn(List.of(mock(de.hallenbelegung.application.domain.model.BlockedTime.class)));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.update(
                        admin.getId(),
                        booking.getId(),
                        fullHall.getId(),
                        "Titel",
                        "Beschreibung",
                        newStart,
                        newEnd
                )
        );

        assertEquals("Conflict with blocked time", exception.getMessage());
    }

    @Test
    void update_ignores_cancelled_bookings_in_conflict_check_for_full_hall() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall currentHall = createPartHallA();
        Hall fullHall = createFullHall();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                currentHall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );
        Booking cancelledBooking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                createPartHallB(),
                LocalDateTime.of(2026, 4, 21, 18, 0),
                LocalDateTime.of(2026, 4, 21, 19, 0)
        );
        cancelledBooking.cancel(admin, "Cancelled");

        LocalDateTime newStart = LocalDateTime.of(2026, 4, 21, 18, 0);
        LocalDateTime newEnd = LocalDateTime.of(2026, 4, 21, 19, 0);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(hallRepository.findById(fullHall.getId())).thenReturn(Optional.of(fullHall));
        when(bookingRepository.findByTimeRange(newStart, newEnd)).thenReturn(List.of(cancelledBooking));
        when(blockedTimeRepository.findAllByTimeRange(newStart, newEnd)).thenReturn(List.of());
        when(bookingRepository.save(booking)).thenReturn(booking);

        Booking result = service.update(
                admin.getId(),
                booking.getId(),
                fullHall.getId(),
                "Titel",
                "Beschreibung",
                newStart,
                newEnd
        );

        assertEquals(fullHall.getId(), result.getHall().getId());
    }

    @Test
    void update_ignores_cancelled_bookings_in_conflict_check_for_same_part_hall() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createPartHallA();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );
        Booking cancelledConflict = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 21, 10, 0),
                LocalDateTime.of(2026, 4, 21, 11, 0)
        );
        cancelledConflict.cancel(owner, "Storniert");

        LocalDateTime newStart = LocalDateTime.of(2026, 4, 21, 10, 0);
        LocalDateTime newEnd = LocalDateTime.of(2026, 4, 21, 11, 0);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));
        when(bookingRepository.findByHallIdAndTimeRange(hall.getId(), newStart, newEnd)).thenReturn(List.of(cancelledConflict));
        when(bookingRepository.findByTimeRange(newStart, newEnd)).thenReturn(List.of(cancelledConflict));
        when(blockedTimeRepository.findByHallIdAndTimeRange(hall.getId(), newStart, newEnd)).thenReturn(List.of());
        when(blockedTimeRepository.findAllByTimeRange(newStart, newEnd)).thenReturn(List.of());
        when(bookingRepository.save(booking)).thenReturn(booking);

        Booking result = service.update(
                admin.getId(),
                booking.getId(),
                hall.getId(),
                "Titel",
                "Beschreibung",
                newStart,
                newEnd
        );

        assertEquals(hall.getId(), result.getHall().getId());
    }

    // ============================================================================
    // KRASSE EDGE CASES - CASCADING RACE CONDITIONS & STATE MACHINE VIOLATIONS
    // ============================================================================

    @Test
    void cancel_then_addFeedback_with_simultaneous_second_cancel_attempt_succeeds_first_fails_second() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createPartHallA();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        service.cancel(admin.getId(), booking.getId(), "Admin Cancel");

        assertTrue(booking.isCancelled());
        assertEquals(admin.getId(), booking.getCancelledBy().getId());

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.cancel(owner.getId(), booking.getId(), "Owner Cancel")
        );

        assertEquals("Booking is already cancelled", ex.getMessage());
        verify(bookingRepository, times(1)).save(any());
    }

    @Test
    void cancel_by_owner_then_admin_cancels_same_booking_preserves_original_cancel_metadata() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createPartHallA();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        service.cancel(owner.getId(), booking.getId(), "Owner Cancellation");

        UUID originalCancelledBy = booking.getCancelledBy().getId();
        String originalReason = booking.getCancelReason();

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.cancel(admin.getId(), booking.getId(), "Admin Override Cancellation")
        );

        assertEquals("Booking is already cancelled", ex.getMessage());
        assertEquals(originalCancelledBy, booking.getCancelledBy().getId());
        assertEquals(originalReason, booking.getCancelReason());
    }

    @Test
    void addFeedback_on_already_cancelled_booking_throws_validation_error_immediately() {
        User owner = createRepresentative();
        Hall hall = createPartHallA();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );
        booking.cancel(owner, "Owner Cancel");

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.addFeedback(owner.getId(), booking.getId(), 42, "Feedback")
        );

        assertEquals("Cannot add feedback to cancelled booking", ex.getMessage());
        verify(bookingRepository, never()).save(any());
        verify(notificationPort, never()).notifyRequesterAboutBookingUpdated(any());
    }

    @Test
    void update_with_feedback_then_cancel_loses_feedback_data_but_preserves_cancel_info() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createPartHallA();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        service.addFeedback(owner.getId(), booking.getId(), 35, "Great turnout!");
        assertEquals(35, booking.getParticipantCount());
        assertEquals("Great turnout!", booking.getFeedbackComment());

        service.cancel(admin.getId(), booking.getId(), "Admin Cancel for maintenance");

        assertTrue(booking.isCancelled());
        assertEquals(admin.getId(), booking.getCancelledBy().getId());
        assertEquals("Admin Cancel for maintenance", booking.getCancelReason());
        // Feedback still accessible after cancel (not cleared in this implementation)
        assertEquals(35, booking.getParticipantCount());
    }

    @Test
    void update_booking_at_exact_year_boundary_accepts_within_one_year_max() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createPartHallA();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 14, 10, 0),
                LocalDateTime.of(2026, 4, 14, 11, 0)
        );

        LocalDateTime exactOneYearFuture = LocalDateTime.of(2027, 4, 14, 9, 45);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));
        when(bookingRepository.findByHallIdAndTimeRange(hall.getId(), exactOneYearFuture, exactOneYearFuture.plusHours(1))).thenReturn(List.of());
        when(bookingRepository.findByTimeRange(exactOneYearFuture, exactOneYearFuture.plusHours(1))).thenReturn(List.of());
        when(blockedTimeRepository.findByHallIdAndTimeRange(hall.getId(), exactOneYearFuture, exactOneYearFuture.plusHours(1))).thenReturn(List.of());
        when(blockedTimeRepository.findAllByTimeRange(exactOneYearFuture, exactOneYearFuture.plusHours(1))).thenReturn(List.of());
        when(bookingRepository.save(booking)).thenReturn(booking);

        Booking result = service.update(
                admin.getId(),
                booking.getId(),
                hall.getId(),
                "Updated Title",
                "Updated Description",
                exactOneYearFuture,
                exactOneYearFuture.plusHours(1)
        );

        assertEquals(exactOneYearFuture, result.getStartAt());
    }

    @Test
    void update_booking_one_minute_over_year_boundary_throws_validation_error() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createPartHallA();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 14, 10, 0),
                LocalDateTime.of(2026, 4, 14, 11, 0)
        );

        // Clock is fixed to 2026-04-14T10:00:00Z, so one year = 2027-04-14T10:00:00Z
        // isAfter checks STRICTLY AFTER, so we need >= one year
        LocalDateTime justOverOneYear = LocalDateTime.of(2027, 4, 15, 10, 0);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.update(
                        admin.getId(),
                        booking.getId(),
                        hall.getId(),
                        "Title",
                        "Desc",
                        justOverOneYear,
                        justOverOneYear.plusHours(1)
                )
        );

        assertEquals("Booking must not be more than one year in advance", ex.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void cancel_with_extremely_long_reason_string_1000_chars() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createPartHallA();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        String veryLongReason = "A".repeat(1000);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        service.cancel(admin.getId(), booking.getId(), veryLongReason);

        assertEquals(veryLongReason, booking.getCancelReason());
        verify(bookingRepository).save(booking);
    }

    @Test
    void addFeedback_with_zero_participants_is_valid() {
        User owner = createRepresentative();
        Hall hall = createPartHallA();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        service.addFeedback(owner.getId(), booking.getId(), 0, "Nobody showed up");

        assertEquals(0, booking.getParticipantCount());
        assertEquals("Nobody showed up", booking.getFeedbackComment());
        verify(bookingRepository).save(booking);
    }

    @Test
    void addFeedback_with_huge_participant_count_integer_max() {
        User owner = createRepresentative();
        Hall hall = createPartHallA();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        service.addFeedback(owner.getId(), booking.getId(), Integer.MAX_VALUE, "Record breaking!");

        assertEquals(Integer.MAX_VALUE, booking.getParticipantCount());
        verify(bookingRepository).save(booking);
    }

    @Test
    void addFeedback_with_null_participant_count_but_with_feedback_comment() {
        User owner = createRepresentative();
        Hall hall = createPartHallA();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        service.addFeedback(owner.getId(), booking.getId(), null, "Very successful event but no count recorded");

        assertNull(booking.getParticipantCount());
        assertEquals("Very successful event but no count recorded", booking.getFeedbackComment());
        verify(bookingRepository).save(booking);
    }

    @Test
    void getById_with_null_currentUserId_returns_minimal_booking_detail_view() {
        Hall hall = createPartHallA();
        User owner = createRepresentative();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        BookingDetailView view = service.getById(null, booking.getId());

        assertNotNull(view);
        assertFalse(view.canViewFeedback());
        assertFalse(view.canEdit());
        assertFalse(view.canCancel());
        assertNull(view.participantCount());
        assertNull(view.feedbackComment());
    }

    @Test
    void getById_admin_sees_all_fields_including_feedback_from_any_booking() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createPartHallA();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );
        booking.addFeedback(42, "Perfect attendance!");

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        BookingDetailView view = service.getById(admin.getId(), booking.getId());

        assertTrue(view.canViewFeedback());
        assertTrue(view.canEdit());
        assertTrue(view.canCancel());
        assertEquals(42, view.participantCount());
        assertEquals("Perfect attendance!", view.feedbackComment());
    }

    @Test
    void getById_non_responsible_user_cannot_edit_or_view_feedback_but_can_view_booking() {
        User admin = createAdmin();
        User owner = createRepresentative();
        User otherUser = spy(createRepresentative());
        Hall hall = createPartHallA();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );
        booking.addFeedback(50, "Secret feedback");

        when(userRepository.findById(otherUser.getId())).thenReturn(Optional.of(otherUser));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        BookingDetailView view = service.getById(otherUser.getId(), booking.getId());

        assertFalse(view.canViewFeedback());
        assertFalse(view.canEdit());
        assertFalse(view.canCancel());
        assertNull(view.participantCount());
        assertNull(view.feedbackComment());
    }

    @Test
    void cancel_by_non_admin_non_owner_throws_forbidden_without_saving() {
        User admin = createAdmin();
        User owner = createRepresentative();
        User otherUser = spy(createRepresentative());
        Hall hall = createPartHallA();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        when(userRepository.findById(otherUser.getId())).thenReturn(Optional.of(otherUser));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        ForbiddenException ex = assertThrows(
                ForbiddenException.class,
                () -> service.cancel(otherUser.getId(), booking.getId(), "Unauthorized cancel")
        );

        assertEquals("User not allowed to cancel this booking", ex.getMessage());
        assertFalse(booking.isCancelled());
        verify(bookingRepository, never()).save(any());
        verify(notificationPort, never()).notifyRequesterAboutBookingCancelledByAdmin(any(), any());
    }

    @Test
    void update_changes_from_part_hall_to_full_hall_correctly() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall partHall = createPartHallA();
        Hall fullHall = createFullHall();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                partHall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        LocalDateTime newStart = LocalDateTime.of(2026, 4, 21, 14, 0);
        LocalDateTime newEnd = LocalDateTime.of(2026, 4, 21, 15, 0);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(hallRepository.findById(fullHall.getId())).thenReturn(Optional.of(fullHall));
        when(bookingRepository.findByTimeRange(newStart, newEnd)).thenReturn(List.of());
        when(blockedTimeRepository.findAllByTimeRange(newStart, newEnd)).thenReturn(List.of());
        when(bookingRepository.save(booking)).thenReturn(booking);

        Booking result = service.update(
                admin.getId(),
                booking.getId(),
                fullHall.getId(),
                "Full Hall Booking",
                "Updated to use full hall",
                newStart,
                newEnd
        );

        assertEquals(fullHall.getId(), result.getHall().getId());
        verify(notificationPort).notifyRequesterAboutBookingUpdated(result);
    }

    @Test
    void update_changes_from_full_hall_to_part_hall_correctly() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall fullHall = createFullHall();
        Hall partHall = createPartHallA();

        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                fullHall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        LocalDateTime newStart = LocalDateTime.of(2026, 4, 21, 14, 0);
        LocalDateTime newEnd = LocalDateTime.of(2026, 4, 21, 15, 0);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(hallRepository.findById(partHall.getId())).thenReturn(Optional.of(partHall));
        when(bookingRepository.findByHallIdAndTimeRange(partHall.getId(), newStart, newEnd)).thenReturn(List.of());
        when(bookingRepository.findByTimeRange(newStart, newEnd)).thenReturn(List.of());
        when(blockedTimeRepository.findByHallIdAndTimeRange(partHall.getId(), newStart, newEnd)).thenReturn(List.of());
        when(blockedTimeRepository.findAllByTimeRange(newStart, newEnd)).thenReturn(List.of());
        when(bookingRepository.save(booking)).thenReturn(booking);

        Booking result = service.update(
                admin.getId(),
                booking.getId(),
                partHall.getId(),
                "Part Hall Booking",
                "Updated to part hall",
                newStart,
                newEnd
        );

        assertEquals(partHall.getId(), result.getHall().getId());
        verify(notificationPort).notifyRequesterAboutBookingUpdated(result);
    }

    @Test
    void getBookingsByUser_returns_sorted_by_start_time_descending() {
        User user = createRepresentative();
        Hall hall = createPartHallA();

        Booking booking1 = createBookingWithId(
                UUID.randomUUID(),
                user,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );
        Booking booking2 = createBookingWithId(
                UUID.randomUUID(),
                user,
                hall,
                LocalDateTime.of(2026, 4, 25, 10, 0),
                LocalDateTime.of(2026, 4, 25, 11, 0)
        );
        Booking booking3 = createBookingWithId(
                UUID.randomUUID(),
                user,
                hall,
                LocalDateTime.of(2026, 4, 22, 10, 0),
                LocalDateTime.of(2026, 4, 22, 11, 0)
        );

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(bookingRepository.findByResponsibleUserId(user.getId())).thenReturn(List.of(booking1, booking3, booking2));

        List<Booking> result = service.getBookingsByUser(user.getId());

        assertEquals(3, result.size());
        assertEquals(LocalDateTime.of(2026, 4, 25, 10, 0), result.get(0).getStartAt());
        assertEquals(LocalDateTime.of(2026, 4, 22, 10, 0), result.get(1).getStartAt());
        assertEquals(LocalDateTime.of(2026, 4, 20, 10, 0), result.get(2).getStartAt());
    }

    @Test
    void getBookingsByUser_returns_empty_list_when_no_bookings() {
        User user = createRepresentative();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(bookingRepository.findByResponsibleUserId(user.getId())).thenReturn(List.of());

        List<Booking> result = service.getBookingsByUser(user.getId());

        assertTrue(result.isEmpty());
    }

    @Test
    void update_sends_notification_after_successful_update() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall currentHall = createPartHallA();
        Hall targetHall = createPartHallB();
        Booking booking = createBookingWithId(
                UUID.randomUUID(),
                owner,
                currentHall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        LocalDateTime start = LocalDateTime.of(2026, 4, 21, 14, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 21, 15, 0);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(hallRepository.findById(targetHall.getId())).thenReturn(Optional.of(targetHall));
        when(bookingRepository.findByHallIdAndTimeRange(targetHall.getId(), start, end)).thenReturn(List.of());
        when(bookingRepository.findByTimeRange(start, end)).thenReturn(List.of());
        when(blockedTimeRepository.findByHallIdAndTimeRange(targetHall.getId(), start, end)).thenReturn(List.of());
        when(blockedTimeRepository.findAllByTimeRange(start, end)).thenReturn(List.of());
        when(bookingRepository.save(booking)).thenReturn(booking);

        service.update(admin.getId(), booking.getId(), targetHall.getId(), "Updated", "desc", start, end);

        verify(notificationPort, times(1)).notifyRequesterAboutBookingUpdated(any(Booking.class));
    }

    @Test
    void cancel_owner_does_not_trigger_admin_notification() {
        User owner = createRepresentative();
        Hall hall = createPartHallA();
        Booking booking = createBooking(
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        service.cancel(owner.getId(), booking.getId(), "reason");

        verify(notificationPort, never()).notifyRequesterAboutBookingCancelledByAdmin(any(Booking.class), any());
    }
}

