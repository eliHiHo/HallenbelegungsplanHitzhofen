package de.hallenbelegung.application.domain.service;

import de.hallenbelegung.application.domain.exception.ForbiddenException;
import de.hallenbelegung.application.domain.exception.NotFoundException;
import de.hallenbelegung.application.domain.exception.ValidationException;
import de.hallenbelegung.application.domain.model.Booking;
import de.hallenbelegung.application.domain.model.BookingSeries;
import de.hallenbelegung.application.domain.model.BookingSeriesStatus;
import de.hallenbelegung.application.domain.model.BookingStatus;
import de.hallenbelegung.application.domain.model.Hall;
import de.hallenbelegung.application.domain.model.HallType;
import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.out.BookingRepositoryPort;
import de.hallenbelegung.application.domain.port.out.BookingSeriesRepositoryPort;
import de.hallenbelegung.application.domain.port.out.NotificationPort;
import de.hallenbelegung.application.domain.port.out.UserRepositoryPort;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BookingSeriesServiceTest {

    private BookingSeriesRepositoryPort bookingSeriesRepository;
    private BookingRepositoryPort bookingRepository;
    private UserRepositoryPort userRepository;
    private NotificationPort notificationPort;

    private BookingSeriesService service;

    @BeforeEach
    void setUp() {
        bookingSeriesRepository = mock(BookingSeriesRepositoryPort.class);
        bookingRepository = mock(BookingRepositoryPort.class);
        userRepository = mock(UserRepositoryPort.class);
        notificationPort = mock(NotificationPort.class);

        service = new BookingSeriesService(
                bookingSeriesRepository,
                bookingRepository,
                userRepository,
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

    private Hall createHall() {
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

    private BookingSeries createSeries(User owner, Hall hall) {
        Instant now = Instant.now();
        return new BookingSeries(
                UUID.randomUUID(),
                "Volleyball Jugend",
                "Serientraining",
                DayOfWeek.MONDAY,
                LocalTime.of(18, 0),
                LocalTime.of(19, 30),
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 7, 20),
                BookingSeriesStatus.ACTIVE,
                hall,
                owner,
                owner,
                owner,
                null,
                now,
                now,
                null,
                null
        );
    }

    private Booking createSeriesBooking(User owner,
                                        Hall hall,
                                        BookingSeries series,
                                        LocalDateTime start,
                                        LocalDateTime end) {
        Instant now = Instant.now();
        return new Booking(
                UUID.randomUUID(),
                "Volleyball Jugend",
                "Einzeltermin",
                start,
                end,
                BookingStatus.APPROVED,
                null,
                false,
                null,
                hall,
                owner,
                series,
                now,
                now,
                owner,
                owner,
                null,
                null,
                null
        );
    }

    private Booking createSingleBooking(User owner,
                                        Hall hall,
                                        LocalDateTime start,
                                        LocalDateTime end) {
        Instant now = Instant.now();
        return new Booking(
                UUID.randomUUID(),
                "Einzeltermin",
                "Kein Serientermin",
                start,
                end,
                BookingStatus.APPROVED,
                null,
                false,
                null,
                hall,
                owner,
                null,
                now,
                now,
                owner,
                owner,
                null,
                null,
                null
        );
    }

    @Test
    void getById_allows_admin() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createHall();
        BookingSeries series = createSeries(owner, hall);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingSeriesRepository.findById(series.getId())).thenReturn(Optional.of(series));

        BookingSeries result = service.getById(admin.getId(), series.getId());

        assertEquals(series.getId(), result.getId());
    }

    @Test
    void getById_allows_owner() {
        User owner = createRepresentative();
        Hall hall = createHall();
        BookingSeries series = createSeries(owner, hall);

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingSeriesRepository.findById(series.getId())).thenReturn(Optional.of(series));

        BookingSeries result = service.getById(owner.getId(), series.getId());

        assertEquals(series.getId(), result.getId());
    }

    @Test
    void getById_rejects_other_user() {
        User owner = createRepresentative();
        User other = createOtherRepresentative();
        Hall hall = createHall();
        BookingSeries series = createSeries(owner, hall);

        when(userRepository.findById(other.getId())).thenReturn(Optional.of(other));
        when(bookingSeriesRepository.findById(series.getId())).thenReturn(Optional.of(series));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.getById(other.getId(), series.getId())
        );

        assertEquals("User not allowed to view this booking series", exception.getMessage());
    }

    @Test
    void getById_rejects_unknown_series() {
        User owner = createRepresentative();

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingSeriesRepository.findById(any())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.getById(owner.getId(), UUID.randomUUID())
        );

        assertEquals("Booking series not found", exception.getMessage());
    }

    @Test
    void getById_rejects_unknown_user() {
        UUID userId = UUID.randomUUID();
        UUID seriesId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.getById(userId, seriesId)
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void getById_rejects_inactive_user() {
        User inactive = createInactiveRepresentative();

        when(userRepository.findById(inactive.getId())).thenReturn(Optional.of(inactive));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.getById(inactive.getId(), UUID.randomUUID())
        );

        assertEquals("User inactive", exception.getMessage());
    }

    @Test
    void getSeriesByUser_returns_only_own_series_sorted_by_createdAt_desc() {
        User owner = createRepresentative();
        Hall hall = createHall();

        Instant olderInstant = Instant.parse("2026-04-01T10:00:00Z");
        Instant newerInstant = Instant.parse("2026-04-05T10:00:00Z");

        BookingSeries older = new BookingSeries(
                UUID.randomUUID(),
                "Alt",
                "Serie alt",
                DayOfWeek.MONDAY,
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 7, 20),
                BookingSeriesStatus.ACTIVE,
                hall,
                owner,
                owner,
                owner,
                null,
                olderInstant,
                olderInstant,
                null,
                null
        );

        BookingSeries newer = new BookingSeries(
                UUID.randomUUID(),
                "Neu",
                "Serie neu",
                DayOfWeek.TUESDAY,
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                LocalDate.of(2026, 4, 21),
                LocalDate.of(2026, 7, 21),
                BookingSeriesStatus.ACTIVE,
                hall,
                owner,
                owner,
                owner,
                null,
                newerInstant,
                newerInstant,
                null,
                null
        );

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingSeriesRepository.findByResponsibleUserId(owner.getId())).thenReturn(List.of(older, newer));

        List<BookingSeries> result = service.getSeriesByUser(owner.getId());

        assertEquals(2, result.size());
        assertEquals(newer.getId(), result.get(0).getId());
        assertEquals(older.getId(), result.get(1).getId());
    }

    @Test
    void getSeriesByUser_rejects_unknown_user() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.getSeriesByUser(userId)
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void getSeriesByUser_rejects_inactive_user() {
        User inactive = createInactiveRepresentative();

        when(userRepository.findById(inactive.getId())).thenReturn(Optional.of(inactive));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.getSeriesByUser(inactive.getId())
        );

        assertEquals("User inactive", exception.getMessage());
    }

    @Test
    void cancelSeries_success_for_owner() {
        User owner = createRepresentative();
        Hall hall = createHall();
        BookingSeries series = createSeries(owner, hall);

        Booking booking1 = createSeriesBooking(
                owner,
                hall,
                series,
                LocalDateTime.of(2026, 4, 20, 18, 0),
                LocalDateTime.of(2026, 4, 20, 19, 30)
        );
        Booking booking2 = createSeriesBooking(
                owner,
                hall,
                series,
                LocalDateTime.of(2026, 4, 27, 18, 0),
                LocalDateTime.of(2026, 4, 27, 19, 30)
        );

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingSeriesRepository.findById(series.getId())).thenReturn(Optional.of(series));
        when(bookingRepository.findByBookingSeriesId(series.getId())).thenReturn(List.of(booking1, booking2));
        when(bookingSeriesRepository.save(series)).thenReturn(series);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.cancelSeries(owner.getId(), series.getId(), "Sommerpause");

        assertTrue(series.isCancelled());
        assertEquals("Sommerpause", series.getCancelReason());
        assertEquals(owner.getId(), series.getCancelledBy().getId());
        assertEquals(owner.getId(), series.getUpdatedBy().getId());
        assertNotNull(series.getCancelledAt());
        assertNotNull(series.getUpdatedAt());

        assertTrue(booking1.isCancelled());
        assertTrue(booking2.isCancelled());
        assertEquals("Sommerpause", booking1.getCancelReason());
        assertEquals("Sommerpause", booking2.getCancelReason());
        assertEquals(owner.getId(), booking1.getCancelledBy().getId());
        assertEquals(owner.getId(), booking2.getCancelledBy().getId());
        assertEquals(owner.getId(), booking1.getUpdatedBy().getId());
        assertEquals(owner.getId(), booking2.getUpdatedBy().getId());
        assertNotNull(booking1.getCancelledAt());
        assertNotNull(booking2.getCancelledAt());
        assertNotNull(booking1.getUpdatedAt());
        assertNotNull(booking2.getUpdatedAt());

        verify(bookingSeriesRepository).save(series);
        verify(bookingRepository).save(booking1);
        verify(bookingRepository).save(booking2);
        verify(notificationPort, never()).notifyRequesterAboutBookingSeriesCancelledByAdmin(any(), any());
    }

    @Test
    void cancelSeries_success_for_admin_and_notifies() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createHall();
        BookingSeries series = createSeries(owner, hall);

        Booking booking1 = createSeriesBooking(
                owner,
                hall,
                series,
                LocalDateTime.of(2026, 4, 20, 18, 0),
                LocalDateTime.of(2026, 4, 20, 19, 30)
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingSeriesRepository.findById(series.getId())).thenReturn(Optional.of(series));
        when(bookingRepository.findByBookingSeriesId(series.getId())).thenReturn(List.of(booking1));
        when(bookingSeriesRepository.save(series)).thenReturn(series);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.cancelSeries(admin.getId(), series.getId(), "Gemeindeveranstaltung");

        assertTrue(series.isCancelled());
        assertEquals("Gemeindeveranstaltung", series.getCancelReason());
        assertEquals(admin.getId(), series.getCancelledBy().getId());
        assertEquals(admin.getId(), series.getUpdatedBy().getId());
        assertNotNull(series.getCancelledAt());
        assertTrue(booking1.isCancelled());
        assertEquals(admin.getId(), booking1.getCancelledBy().getId());
        assertEquals(admin.getId(), booking1.getUpdatedBy().getId());
        assertEquals("Gemeindeveranstaltung", booking1.getCancelReason());
        assertNotNull(booking1.getCancelledAt());

        verify(notificationPort)
                .notifyRequesterAboutBookingSeriesCancelledByAdmin(series, "Gemeindeveranstaltung");
    }

    @Test
    void cancelSeries_accepts_null_reason() {
        User owner = createRepresentative();
        Hall hall = createHall();
        BookingSeries series = createSeries(owner, hall);

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingSeriesRepository.findById(series.getId())).thenReturn(Optional.of(series));
        when(bookingRepository.findByBookingSeriesId(series.getId())).thenReturn(List.of());
        when(bookingSeriesRepository.save(series)).thenReturn(series);

        service.cancelSeries(owner.getId(), series.getId(), null);

        assertTrue(series.isCancelled());
        assertNull(series.getCancelReason());
    }

    @Test
    void cancelSeries_rejects_unknown_user() {
        UUID userId = UUID.randomUUID();
        UUID seriesId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.cancelSeries(userId, seriesId, "Reason")
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void cancelSeries_rejects_inactive_user() {
        User inactive = createInactiveRepresentative();

        when(userRepository.findById(inactive.getId())).thenReturn(Optional.of(inactive));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.cancelSeries(inactive.getId(), UUID.randomUUID(), "Reason")
        );

        assertEquals("User inactive", exception.getMessage());
    }

    @Test
    void cancelSeries_rejects_unknown_series() {
        User owner = createRepresentative();

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingSeriesRepository.findById(any())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.cancelSeries(owner.getId(), UUID.randomUUID(), "Reason")
        );

        assertEquals("Booking series not found", exception.getMessage());
    }

    @Test
    void cancelSeries_rejects_other_user() {
        User owner = createRepresentative();
        User other = createOtherRepresentative();
        Hall hall = createHall();
        BookingSeries series = createSeries(owner, hall);

        when(userRepository.findById(other.getId())).thenReturn(Optional.of(other));
        when(bookingSeriesRepository.findById(series.getId())).thenReturn(Optional.of(series));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.cancelSeries(other.getId(), series.getId(), "Nope")
        );

        assertEquals("User not allowed to cancel this booking series", exception.getMessage());
    }

    @Test
    void cancelSeries_rejects_already_cancelled_series() {
        User owner = createRepresentative();
        Hall hall = createHall();
        BookingSeries series = createSeries(owner, hall);
        series.cancel(owner, "Schon storniert");

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingSeriesRepository.findById(series.getId())).thenReturn(Optional.of(series));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.cancelSeries(owner.getId(), series.getId(), "Nochmal")
        );

        assertEquals("Booking series is already cancelled", exception.getMessage());
        verify(bookingRepository, never()).findByBookingSeriesId(any());
        verify(bookingSeriesRepository, never()).save(any());
        verify(notificationPort, never()).notifyRequesterAboutBookingSeriesCancelledByAdmin(any(), any());
    }

    @Test
    void cancelSeries_skips_already_cancelled_occurrences() {
        User owner = createRepresentative();
        Hall hall = createHall();
        BookingSeries series = createSeries(owner, hall);

        Booking booking1 = createSeriesBooking(
                owner,
                hall,
                series,
                LocalDateTime.of(2026, 4, 20, 18, 0),
                LocalDateTime.of(2026, 4, 20, 19, 30)
        );
        Booking booking2 = createSeriesBooking(
                owner,
                hall,
                series,
                LocalDateTime.of(2026, 4, 27, 18, 0),
                LocalDateTime.of(2026, 4, 27, 19, 30)
        );
        booking2.cancel(owner, "Bereits storniert");

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingSeriesRepository.findById(series.getId())).thenReturn(Optional.of(series));
        when(bookingRepository.findByBookingSeriesId(series.getId())).thenReturn(List.of(booking1, booking2));
        when(bookingSeriesRepository.save(series)).thenReturn(series);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.cancelSeries(owner.getId(), series.getId(), "Alles absagen");

        assertTrue(booking1.isCancelled());
        assertTrue(booking2.isCancelled());

        verify(bookingRepository).save(booking1);
        verify(bookingRepository, never()).save(booking2);
    }

    @Test
    void cancelSeries_rejects_other_user_without_loading_bookings_or_saving() {
        User owner = createRepresentative();
        User other = createOtherRepresentative();
        Hall hall = createHall();
        BookingSeries series = createSeries(owner, hall);

        when(userRepository.findById(other.getId())).thenReturn(Optional.of(other));
        when(bookingSeriesRepository.findById(series.getId())).thenReturn(Optional.of(series));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.cancelSeries(other.getId(), series.getId(), "Nope")
        );

        assertEquals("User not allowed to cancel this booking series", exception.getMessage());
        verify(bookingRepository, never()).findByBookingSeriesId(any());
        verify(bookingSeriesRepository, never()).save(any());
        verify(bookingRepository, never()).save(any());
        verify(notificationPort, never()).notifyRequesterAboutBookingSeriesCancelledByAdmin(any(), any());
    }

    @Test
    void cancelSingleOccurrence_success_for_owner() {
        User owner = createRepresentative();
        Hall hall = createHall();
        BookingSeries series = createSeries(owner, hall);

        Booking selected = createSeriesBooking(
                owner,
                hall,
                series,
                LocalDateTime.of(2026, 4, 20, 18, 0),
                LocalDateTime.of(2026, 4, 20, 19, 30)
        );

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingSeriesRepository.findById(series.getId())).thenReturn(Optional.of(series));
        when(bookingRepository.findById(selected.getId())).thenReturn(Optional.of(selected));
        when(bookingRepository.save(selected)).thenReturn(selected);

        service.cancelSingleOccurrence(owner.getId(), series.getId(), selected.getId(), "Einmaliger Ausfall");

        assertTrue(selected.isCancelled());
        assertEquals("Einmaliger Ausfall", selected.getCancelReason());
        assertEquals(owner.getId(), selected.getCancelledBy().getId());
        assertEquals(owner.getId(), selected.getUpdatedBy().getId());
        assertNotNull(selected.getCancelledAt());
        assertNotNull(selected.getUpdatedAt());

        verify(bookingRepository).save(selected);
        verify(notificationPort, never()).notifyRequesterAboutBookingCancelledByAdmin(any(), any());
    }

    @Test
    void cancelSingleOccurrence_success_for_admin() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createHall();
        BookingSeries series = createSeries(owner, hall);

        Booking selected = createSeriesBooking(
                owner,
                hall,
                series,
                LocalDateTime.of(2026, 4, 20, 18, 0),
                LocalDateTime.of(2026, 4, 20, 19, 30)
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingSeriesRepository.findById(series.getId())).thenReturn(Optional.of(series));
        when(bookingRepository.findById(selected.getId())).thenReturn(Optional.of(selected));
        when(bookingRepository.save(selected)).thenReturn(selected);

        service.cancelSingleOccurrence(admin.getId(), series.getId(), selected.getId(), "Sperrung");

        assertTrue(selected.isCancelled());
        assertEquals("Sperrung", selected.getCancelReason());
        assertEquals(admin.getId(), selected.getCancelledBy().getId());
        assertEquals(admin.getId(), selected.getUpdatedBy().getId());
        assertNotNull(selected.getCancelledAt());
    }

    @Test
    void cancelSingleOccurrence_accepts_null_reason() {
        User owner = createRepresentative();
        Hall hall = createHall();
        BookingSeries series = createSeries(owner, hall);

        Booking selected = createSeriesBooking(
                owner,
                hall,
                series,
                LocalDateTime.of(2026, 4, 20, 18, 0),
                LocalDateTime.of(2026, 4, 20, 19, 30)
        );

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingSeriesRepository.findById(series.getId())).thenReturn(Optional.of(series));
        when(bookingRepository.findById(selected.getId())).thenReturn(Optional.of(selected));
        when(bookingRepository.save(selected)).thenReturn(selected);

        service.cancelSingleOccurrence(owner.getId(), series.getId(), selected.getId(), null);

        assertTrue(selected.isCancelled());
        assertNull(selected.getCancelReason());
    }

    @Test
    void cancelSingleOccurrence_rejects_unknown_user() {
        UUID userId = UUID.randomUUID();
        UUID seriesId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.cancelSingleOccurrence(userId, seriesId, bookingId, "Reason")
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void cancelSingleOccurrence_rejects_inactive_user() {
        User inactive = createInactiveRepresentative();

        when(userRepository.findById(inactive.getId())).thenReturn(Optional.of(inactive));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.cancelSingleOccurrence(
                        inactive.getId(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Reason"
                )
        );

        assertEquals("User inactive", exception.getMessage());
    }

    @Test
    void cancelSingleOccurrence_rejects_unknown_series() {
        User owner = createRepresentative();

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingSeriesRepository.findById(any())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.cancelSingleOccurrence(
                        owner.getId(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Reason"
                )
        );

        assertEquals("Booking series not found", exception.getMessage());
    }

    @Test
    void cancelSingleOccurrence_rejects_unknown_booking() {
        User owner = createRepresentative();
        Hall hall = createHall();
        BookingSeries series = createSeries(owner, hall);

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingSeriesRepository.findById(series.getId())).thenReturn(Optional.of(series));
        when(bookingRepository.findById(any())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.cancelSingleOccurrence(owner.getId(), series.getId(), UUID.randomUUID(), "Reason")
        );

        assertEquals("Booking not found", exception.getMessage());
    }

    @Test
    void cancelSingleOccurrence_rejects_other_user() {
        User owner = createRepresentative();
        User other = createOtherRepresentative();
        Hall hall = createHall();
        BookingSeries series = createSeries(owner, hall);

        Booking selected = createSeriesBooking(
                owner,
                hall,
                series,
                LocalDateTime.of(2026, 4, 20, 18, 0),
                LocalDateTime.of(2026, 4, 20, 19, 30)
        );

        when(userRepository.findById(other.getId())).thenReturn(Optional.of(other));
        when(bookingSeriesRepository.findById(series.getId())).thenReturn(Optional.of(series));
        when(bookingRepository.findById(selected.getId())).thenReturn(Optional.of(selected));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.cancelSingleOccurrence(other.getId(), series.getId(), selected.getId(), "Nope")
        );

        assertEquals("User not allowed to cancel occurrences of this booking series", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void cancelSingleOccurrence_rejects_other_user_without_loading_booking() {
        User owner = createRepresentative();
        User other = createOtherRepresentative();
        Hall hall = createHall();
        BookingSeries series = createSeries(owner, hall);
        Booking selected = createSeriesBooking(
                owner,
                hall,
                series,
                LocalDateTime.of(2026, 4, 20, 18, 0),
                LocalDateTime.of(2026, 4, 20, 19, 30)
        );

        when(userRepository.findById(other.getId())).thenReturn(Optional.of(other));
        when(bookingSeriesRepository.findById(series.getId())).thenReturn(Optional.of(series));
        when(bookingRepository.findById(selected.getId())).thenReturn(Optional.of(selected));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.cancelSingleOccurrence(other.getId(), series.getId(), selected.getId(), "Nope")
        );

        assertEquals("User not allowed to cancel occurrences of this booking series", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void cancelSingleOccurrence_rejects_if_series_already_cancelled() {
        User owner = createRepresentative();
        Hall hall = createHall();
        BookingSeries series = createSeries(owner, hall);
        series.cancel(owner, "Serie beendet");

        Booking selected = createSeriesBooking(
                owner,
                hall,
                series,
                LocalDateTime.of(2026, 4, 20, 18, 0),
                LocalDateTime.of(2026, 4, 20, 19, 30)
        );

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingSeriesRepository.findById(series.getId())).thenReturn(Optional.of(series));
        when(bookingRepository.findById(selected.getId())).thenReturn(Optional.of(selected));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.cancelSingleOccurrence(owner.getId(), series.getId(), selected.getId(), "Reason")
        );

        assertEquals("Cannot cancel a single occurrence of an already cancelled booking series", exception.getMessage());
    }

    @Test
    void cancelSingleOccurrence_rejects_booking_without_series() {
        User owner = createRepresentative();
        Hall hall = createHall();
        BookingSeries series = createSeries(owner, hall);

        Booking singleBooking = createSingleBooking(
                owner,
                hall,
                LocalDateTime.of(2026, 4, 20, 18, 0),
                LocalDateTime.of(2026, 4, 20, 19, 30)
        );

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingSeriesRepository.findById(series.getId())).thenReturn(Optional.of(series));
        when(bookingRepository.findById(singleBooking.getId())).thenReturn(Optional.of(singleBooking));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.cancelSingleOccurrence(owner.getId(), series.getId(), singleBooking.getId(), "Reason")
        );

        assertEquals("Booking does not belong to a booking series", exception.getMessage());
    }

    @Test
    void cancelSingleOccurrence_rejects_booking_of_other_series() {
        User owner = createRepresentative();
        Hall hall = createHall();
        BookingSeries seriesA = createSeries(owner, hall);
        BookingSeries seriesB = createSeries(owner, hall);

        Booking bookingOfOtherSeries = createSeriesBooking(
                owner,
                hall,
                seriesB,
                LocalDateTime.of(2026, 4, 20, 18, 0),
                LocalDateTime.of(2026, 4, 20, 19, 30)
        );

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingSeriesRepository.findById(seriesA.getId())).thenReturn(Optional.of(seriesA));
        when(bookingRepository.findById(bookingOfOtherSeries.getId())).thenReturn(Optional.of(bookingOfOtherSeries));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.cancelSingleOccurrence(owner.getId(), seriesA.getId(), bookingOfOtherSeries.getId(), "Reason")
        );

        assertEquals("Booking does not belong to the specified booking series", exception.getMessage());
    }

    @Test
    void cancelSingleOccurrence_rejects_already_cancelled_occurrence() {
        User owner = createRepresentative();
        Hall hall = createHall();
        BookingSeries series = createSeries(owner, hall);

        Booking selected = createSeriesBooking(
                owner,
                hall,
                series,
                LocalDateTime.of(2026, 4, 20, 18, 0),
                LocalDateTime.of(2026, 4, 20, 19, 30)
        );
        selected.cancel(owner, "Schon storniert");

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingSeriesRepository.findById(series.getId())).thenReturn(Optional.of(series));
        when(bookingRepository.findById(selected.getId())).thenReturn(Optional.of(selected));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.cancelSingleOccurrence(owner.getId(), series.getId(), selected.getId(), "Nochmal")
        );

        assertEquals("Booking occurrence is already cancelled", exception.getMessage());
    }

    @Test
    void cancelSingleOccurrence_only_cancels_selected_occurrence() {
        User owner = createRepresentative();
        Hall hall = createHall();
        BookingSeries series = createSeries(owner, hall);

        Booking selected = createSeriesBooking(
                owner,
                hall,
                series,
                LocalDateTime.of(2026, 4, 20, 18, 0),
                LocalDateTime.of(2026, 4, 20, 19, 30)
        );
        Booking other = createSeriesBooking(
                owner,
                hall,
                series,
                LocalDateTime.of(2026, 4, 27, 18, 0),
                LocalDateTime.of(2026, 4, 27, 19, 30)
        );

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingSeriesRepository.findById(series.getId())).thenReturn(Optional.of(series));
        when(bookingRepository.findById(selected.getId())).thenReturn(Optional.of(selected));
        when(bookingRepository.save(selected)).thenReturn(selected);

        service.cancelSingleOccurrence(owner.getId(), series.getId(), selected.getId(), "Nur dieser Termin");

        assertTrue(selected.isCancelled());
        assertFalse(other.isCancelled());
    }
}