package de.hallenbelegung.application.domain.service;

import de.hallenbelegung.application.domain.exception.BookingConflictException;
import de.hallenbelegung.application.domain.exception.ForbiddenException;
import de.hallenbelegung.application.domain.exception.NotFoundException;
import de.hallenbelegung.application.domain.exception.ValidationException;
import de.hallenbelegung.application.domain.model.BlockedTime;
import de.hallenbelegung.application.domain.model.BlockedTimeType;
import de.hallenbelegung.application.domain.model.Booking;
import de.hallenbelegung.application.domain.model.BookingSeries;
import de.hallenbelegung.application.domain.model.BookingStatus;
import de.hallenbelegung.application.domain.model.Hall;
import de.hallenbelegung.application.domain.model.HallType;
import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.out.BlockedTimeRepositoryPort;
import de.hallenbelegung.application.domain.port.out.BookingRepositoryPort;
import de.hallenbelegung.application.domain.port.out.HallConfigPort;
import de.hallenbelegung.application.domain.port.out.HallRepositoryPort;
import de.hallenbelegung.application.domain.port.out.PublicHolidayPort;
import de.hallenbelegung.application.domain.port.out.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BlockedTimeServiceTest {

    private BlockedTimeRepositoryPort blockedTimeRepository;
    private BookingRepositoryPort bookingRepository;
    private UserRepositoryPort userRepository;
    private HallRepositoryPort hallRepository;
    private HallConfigPort config;
    private PublicHolidayPort publicHolidayPort;
    private Clock clock;

    private BlockedTimeService service;

    @BeforeEach
    void setUp() {
        blockedTimeRepository = mock(BlockedTimeRepositoryPort.class);
        bookingRepository = mock(BookingRepositoryPort.class);
        userRepository = mock(UserRepositoryPort.class);
        hallRepository = mock(HallRepositoryPort.class);
        config = mock(HallConfigPort.class);
        publicHolidayPort = mock(PublicHolidayPort.class);

        clock = Clock.fixed(
                Instant.parse("2026-04-14T10:00:00Z"),
                ZoneId.of("Europe/Berlin")
        );

        when(config.bookingIntervalMinutes()).thenReturn(15);
        when(config.openingStart()).thenReturn(LocalTime.of(8, 0));
        when(config.openingEnd()).thenReturn(LocalTime.of(22, 0));

        service = new BlockedTimeService(
                blockedTimeRepository,
                bookingRepository,
                userRepository,
                hallRepository,
                config,
                clock,
                publicHolidayPort
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

    private User createInactiveAdmin() {
        return new User(
                UUID.randomUUID(),
                "Inactive",
                "Admin",
                "inactive-admin@example.com",
                "hash",
                Role.ADMIN,
                false,
                Instant.now(),
                Instant.now()
        );
    }

    private Hall createFullHall() {
        return new Hall(
                UUID.randomUUID(),
                "Gesamthalle",
                "Die ganze Halle",
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
                "Inaktiv",
                "Inaktive Halle",
                false,
                Instant.now(),
                Instant.now(),
                HallType.PART_SMALL
        );
    }

    private Booking createBooking(User owner, Hall hall, LocalDateTime start, LocalDateTime end) {
        Instant now = Instant.now();
        return new Booking(
                UUID.randomUUID(),
                "Volleyball",
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
                now,
                now,
                owner,
                owner,
                null,
                null,
                null
        );
    }

    private BlockedTime createBlockedTime(
            Hall hall,
            User admin,
            LocalDateTime start,
            LocalDateTime end,
            BlockedTimeType type
    ) {
        return new BlockedTime(
                UUID.randomUUID(),
                type == BlockedTimeType.PUBLIC_HOLIDAY ? "Öffentlicher Feiertag" : "Wartung",
                start,
                end,
                type,
                hall,
                admin,
                admin,
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    void create_success_for_admin_single_day_inside_opening_hours() {
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
        when(blockedTimeRepository.save(any(BlockedTime.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> service.create(hall.getId(), "Wartung", start, end, admin.getId()));

        verify(blockedTimeRepository).save(any(BlockedTime.class));
    }

    @Test
    void create_success_for_admin_single_day_on_exact_opening_boundaries() {
        User admin = createAdmin();
        Hall hall = createPartHallA();
        LocalDateTime start = LocalDateTime.of(2026, 4, 20, 8, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 20, 22, 0);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));
        when(bookingRepository.findByHallIdAndTimeRange(hall.getId(), start, end)).thenReturn(List.of());
        when(bookingRepository.findByTimeRange(start, end)).thenReturn(List.of());
        when(blockedTimeRepository.findByHallIdAndTimeRange(hall.getId(), start, end)).thenReturn(List.of());
        when(blockedTimeRepository.findAllByTimeRange(start, end)).thenReturn(List.of());
        when(blockedTimeRepository.save(any(BlockedTime.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> service.create(hall.getId(), "Wartung", start, end, admin.getId()));

        verify(blockedTimeRepository).save(any(BlockedTime.class));
    }

    @Test
    void create_success_for_admin_multi_day_without_opening_hours_check() {
        User admin = createAdmin();
        Hall hall = createPartHallA();
        LocalDateTime start = LocalDateTime.of(2026, 4, 20, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 22, 0, 0);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));
        when(bookingRepository.findByHallIdAndTimeRange(hall.getId(), start, end)).thenReturn(List.of());
        when(bookingRepository.findByTimeRange(start, end)).thenReturn(List.of());
        when(blockedTimeRepository.findByHallIdAndTimeRange(hall.getId(), start, end)).thenReturn(List.of());
        when(blockedTimeRepository.findAllByTimeRange(start, end)).thenReturn(List.of());
        when(blockedTimeRepository.save(any(BlockedTime.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> service.create(hall.getId(), "Renovierung", start, end, admin.getId()));

        verify(blockedTimeRepository).save(any(BlockedTime.class));
    }

    @Test
    void create_rejects_unknown_admin() {
        UUID adminId = UUID.randomUUID();

        when(userRepository.findById(adminId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.create(
                        UUID.randomUUID(),
                        "Wartung",
                        LocalDateTime.of(2026, 4, 20, 10, 0),
                        LocalDateTime.of(2026, 4, 20, 11, 0),
                        adminId
                )
        );

        assertEquals("Admin user not found", exception.getMessage());
    }

    @Test
    void create_rejects_non_admin() {
        User representative = createRepresentative();
        Hall hall = createPartHallA();

        when(userRepository.findById(representative.getId())).thenReturn(Optional.of(representative));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.create(
                        hall.getId(),
                        "Wartung",
                        LocalDateTime.of(2026, 4, 20, 10, 0),
                        LocalDateTime.of(2026, 4, 20, 11, 0),
                        representative.getId()
                )
        );

        assertEquals("User not allowed to create blocked times", exception.getMessage());
        verify(hallRepository, never()).findById(any());
        verify(blockedTimeRepository, never()).save(any());
    }

    @Test
    void create_rejects_inactive_admin() {
        User inactiveAdmin = createInactiveAdmin();
        Hall hall = createPartHallA();

        when(userRepository.findById(inactiveAdmin.getId())).thenReturn(Optional.of(inactiveAdmin));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.create(
                        hall.getId(),
                        "Wartung",
                        LocalDateTime.of(2026, 4, 20, 10, 0),
                        LocalDateTime.of(2026, 4, 20, 11, 0),
                        inactiveAdmin.getId()
                )
        );

        assertEquals("Admin user inactive", exception.getMessage());
        verify(hallRepository, never()).findById(any());
        verify(blockedTimeRepository, never()).save(any());
    }

    @Test
    void create_rejects_unknown_hall() {
        User admin = createAdmin();
        UUID hallId = UUID.randomUUID();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(hallRepository.findById(hallId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.create(
                        hallId,
                        "Wartung",
                        LocalDateTime.of(2026, 4, 20, 10, 0),
                        LocalDateTime.of(2026, 4, 20, 11, 0),
                        admin.getId()
                )
        );

        assertEquals("Hall not found", exception.getMessage());
    }

    @Test
    void create_rejects_inactive_hall() {
        User admin = createAdmin();
        Hall inactiveHall = createInactiveHall();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(hallRepository.findById(inactiveHall.getId())).thenReturn(Optional.of(inactiveHall));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.create(
                        inactiveHall.getId(),
                        "Wartung",
                        LocalDateTime.of(2026, 4, 20, 10, 0),
                        LocalDateTime.of(2026, 4, 20, 11, 0),
                        admin.getId()
                )
        );

        assertEquals("Hall inactive", exception.getMessage());
    }

    @Test
    void create_rejects_blank_reason() {
        User admin = createAdmin();
        Hall hall = createPartHallA();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.create(
                        hall.getId(),
                        "   ",
                        LocalDateTime.of(2026, 4, 20, 10, 0),
                        LocalDateTime.of(2026, 4, 20, 11, 0),
                        admin.getId()
                )
        );

        assertEquals("Reason is required", exception.getMessage());
    }

    @Test
    void create_rejects_null_times() {
        User admin = createAdmin();
        Hall hall = createPartHallA();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.create(hall.getId(), "Wartung", null, null, admin.getId())
        );

        assertEquals("Start time and end time are required", exception.getMessage());
    }

    @Test
    void create_rejects_start_after_end() {
        User admin = createAdmin();
        Hall hall = createPartHallA();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.create(
                        hall.getId(),
                        "Wartung",
                        LocalDateTime.of(2026, 4, 20, 12, 0),
                        LocalDateTime.of(2026, 4, 20, 11, 0),
                        admin.getId()
                )
        );

        assertEquals("Start time must be before end time", exception.getMessage());
    }

    @Test
    void create_rejects_start_in_past() {
        User admin = createAdmin();
        Hall hall = createPartHallA();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.create(
                        hall.getId(),
                        "Wartung",
                        LocalDateTime.of(2026, 4, 13, 10, 0),
                        LocalDateTime.of(2026, 4, 13, 11, 0),
                        admin.getId()
                )
        );

        assertEquals("Blocked time cannot start in the past", exception.getMessage());
    }

    @Test
    void create_rejects_invalid_time_grid() {
        User admin = createAdmin();
        Hall hall = createPartHallA();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.create(
                        hall.getId(),
                        "Wartung",
                        LocalDateTime.of(2026, 4, 20, 10, 10),
                        LocalDateTime.of(2026, 4, 20, 11, 0),
                        admin.getId()
                )
        );

        assertEquals("Times must match the allowed booking interval", exception.getMessage());
    }

    @Test
    void create_rejects_seconds_and_nanoseconds() {
        User admin = createAdmin();
        Hall hall = createPartHallA();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.create(
                        hall.getId(),
                        "Wartung",
                        LocalDateTime.of(2026, 4, 20, 10, 15, 1),
                        LocalDateTime.of(2026, 4, 20, 11, 0),
                        admin.getId()
                )
        );

        assertEquals("Seconds and nanoseconds are not allowed", exception.getMessage());
    }

    @Test
    void create_rejects_single_day_outside_opening_hours() {
        User admin = createAdmin();
        Hall hall = createPartHallA();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.create(
                        hall.getId(),
                        "Wartung",
                        LocalDateTime.of(2026, 4, 20, 7, 45),
                        LocalDateTime.of(2026, 4, 20, 9, 0),
                        admin.getId()
                )
        );

        assertEquals("Blocked time is outside opening hours", exception.getMessage());
    }

    @Test
    void create_rejects_conflict_with_existing_booking_same_part_hall() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createPartHallA();
        LocalDateTime start = LocalDateTime.of(2026, 4, 20, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 20, 11, 0);
        Booking booking = createBooking(owner, hall, start, end);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));
        when(bookingRepository.findByHallIdAndTimeRange(hall.getId(), start, end)).thenReturn(List.of(booking));

        BookingConflictException exception = assertThrows(
                BookingConflictException.class,
                () -> service.create(hall.getId(), "Wartung", start, end, admin.getId())
        );

        assertEquals("Conflict with existing booking", exception.getMessage());
        verify(blockedTimeRepository, never()).save(any());
    }

    @Test
    void create_prioritizes_booking_conflict_over_blocked_time_conflict() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createPartHallA();
        LocalDateTime start = LocalDateTime.of(2026, 4, 20, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 20, 11, 0);
        Booking booking = createBooking(owner, hall, start, end);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));
        when(bookingRepository.findByHallIdAndTimeRange(hall.getId(), start, end)).thenReturn(List.of(booking));

        BookingConflictException exception = assertThrows(
                BookingConflictException.class,
                () -> service.create(hall.getId(), "Wartung", start, end, admin.getId())
        );

        assertEquals("Conflict with existing booking", exception.getMessage());
        verify(blockedTimeRepository, never()).findByHallIdAndTimeRange(any(UUID.class), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void create_rejects_conflict_with_existing_full_hall_booking() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall partHall = createPartHallA();
        Hall fullHall = createFullHall();
        LocalDateTime start = LocalDateTime.of(2026, 4, 20, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 20, 11, 0);
        Booking fullHallBooking = createBooking(owner, fullHall, start, end);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(hallRepository.findById(partHall.getId())).thenReturn(Optional.of(partHall));
        when(bookingRepository.findByHallIdAndTimeRange(partHall.getId(), start, end)).thenReturn(List.of());
        when(bookingRepository.findByTimeRange(start, end)).thenReturn(List.of(fullHallBooking));

        BookingConflictException exception = assertThrows(
                BookingConflictException.class,
                () -> service.create(partHall.getId(), "Wartung", start, end, admin.getId())
        );

        assertEquals("Conflict with full hall booking", exception.getMessage());
    }

    @Test
    void create_rejects_conflict_with_existing_booking_when_requesting_full_hall() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall fullHall = createFullHall();
        Hall partHall = createPartHallA();
        LocalDateTime start = LocalDateTime.of(2026, 4, 20, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 20, 11, 0);
        Booking partBooking = createBooking(owner, partHall, start, end);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(hallRepository.findById(fullHall.getId())).thenReturn(Optional.of(fullHall));
        when(bookingRepository.findByTimeRange(start, end)).thenReturn(List.of(partBooking));

        BookingConflictException exception = assertThrows(
                BookingConflictException.class,
                () -> service.create(fullHall.getId(), "Wartung", start, end, admin.getId())
        );

        assertEquals("Conflict with existing booking", exception.getMessage());
    }

    @Test
    void create_ignores_cancelled_booking_conflicts() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createPartHallA();
        LocalDateTime start = LocalDateTime.of(2026, 4, 20, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 20, 11, 0);
        Booking cancelledBooking = createBooking(owner, hall, start, end);
        cancelledBooking.cancel(owner, "Cancelled");

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));
        when(bookingRepository.findByHallIdAndTimeRange(hall.getId(), start, end)).thenReturn(List.of(cancelledBooking));
        when(bookingRepository.findByTimeRange(start, end)).thenReturn(List.of(cancelledBooking));
        when(blockedTimeRepository.findByHallIdAndTimeRange(hall.getId(), start, end)).thenReturn(List.of());
        when(blockedTimeRepository.findAllByTimeRange(start, end)).thenReturn(List.of());
        when(blockedTimeRepository.save(any(BlockedTime.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> service.create(hall.getId(), "Wartung", start, end, admin.getId()));
    }

    @Test
    void create_rejects_conflict_with_existing_blocked_time_same_part_hall() {
        User admin = createAdmin();
        Hall hall = createPartHallA();
        LocalDateTime start = LocalDateTime.of(2026, 4, 20, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 20, 11, 0);
        BlockedTime existing = createBlockedTime(hall, admin, start, end, BlockedTimeType.MANUAL);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));
        when(bookingRepository.findByHallIdAndTimeRange(hall.getId(), start, end)).thenReturn(List.of());
        when(bookingRepository.findByTimeRange(start, end)).thenReturn(List.of());
        when(blockedTimeRepository.findByHallIdAndTimeRange(hall.getId(), start, end)).thenReturn(List.of(existing));

        BookingConflictException exception = assertThrows(
                BookingConflictException.class,
                () -> service.create(hall.getId(), "Wartung", start, end, admin.getId())
        );

        assertEquals("Conflict with existing blocked time", exception.getMessage());
        verify(blockedTimeRepository, never()).save(any());
    }

    @Test
    void create_rejects_conflict_with_full_hall_blocked_time() {
        User admin = createAdmin();
        Hall partHall = createPartHallA();
        Hall fullHall = createFullHall();
        LocalDateTime start = LocalDateTime.of(2026, 4, 20, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 20, 11, 0);
        BlockedTime fullHallBlocked = createBlockedTime(fullHall, admin, start, end, BlockedTimeType.MANUAL);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(hallRepository.findById(partHall.getId())).thenReturn(Optional.of(partHall));
        when(bookingRepository.findByHallIdAndTimeRange(partHall.getId(), start, end)).thenReturn(List.of());
        when(bookingRepository.findByTimeRange(start, end)).thenReturn(List.of());
        when(blockedTimeRepository.findByHallIdAndTimeRange(partHall.getId(), start, end)).thenReturn(List.of());
        when(blockedTimeRepository.findAllByTimeRange(start, end)).thenReturn(List.of(fullHallBlocked));

        BookingConflictException exception = assertThrows(
                BookingConflictException.class,
                () -> service.create(partHall.getId(), "Wartung", start, end, admin.getId())
        );

        assertEquals("Conflict with full hall blocked time", exception.getMessage());
        verify(blockedTimeRepository, never()).save(any());
    }

    @Test
    void create_rejects_conflict_with_any_blocked_time_when_requesting_full_hall() {
        User admin = createAdmin();
        Hall fullHall = createFullHall();
        Hall partHall = createPartHallA();
        LocalDateTime start = LocalDateTime.of(2026, 4, 20, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 20, 11, 0);
        BlockedTime partBlocked = createBlockedTime(partHall, admin, start, end, BlockedTimeType.MANUAL);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(hallRepository.findById(fullHall.getId())).thenReturn(Optional.of(fullHall));
        when(bookingRepository.findByTimeRange(start, end)).thenReturn(List.of());
        when(blockedTimeRepository.findAllByTimeRange(start, end)).thenReturn(List.of(partBlocked));

        BookingConflictException exception = assertThrows(
                BookingConflictException.class,
                () -> service.create(fullHall.getId(), "Wartung", start, end, admin.getId())
        );

        assertEquals("Conflict with existing blocked time", exception.getMessage());
        verify(blockedTimeRepository, never()).save(any());
    }

    @Test
    void getAll_returns_sorted_blocked_times_for_admin() {
        User admin = createAdmin();
        Hall hall = createPartHallA();
        BlockedTime later = createBlockedTime(
                hall,
                admin,
                LocalDateTime.of(2026, 4, 22, 10, 0),
                LocalDateTime.of(2026, 4, 22, 11, 0),
                BlockedTimeType.MANUAL
        );
        BlockedTime earlier = createBlockedTime(
                hall,
                admin,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0),
                BlockedTimeType.MANUAL
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(blockedTimeRepository.findAll()).thenReturn(List.of(later, earlier));

        List<BlockedTime> result = service.getAll(admin.getId());

        assertEquals(2, result.size());
        assertEquals(earlier.getId(), result.get(0).getId());
        assertEquals(later.getId(), result.get(1).getId());
    }

    @Test
    void getAll_rejects_unknown_admin() {
        UUID adminId = UUID.randomUUID();

        when(userRepository.findById(adminId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.getAll(adminId)
        );

        assertEquals("Admin user not found", exception.getMessage());
    }

    @Test
    void getAll_rejects_non_admin() {
        User representative = createRepresentative();

        when(userRepository.findById(representative.getId())).thenReturn(Optional.of(representative));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.getAll(representative.getId())
        );

        assertEquals("User not allowed to view blocked times", exception.getMessage());
    }

    @Test
    void getAll_rejects_inactive_admin() {
        User inactiveAdmin = createInactiveAdmin();

        when(userRepository.findById(inactiveAdmin.getId())).thenReturn(Optional.of(inactiveAdmin));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.getAll(inactiveAdmin.getId())
        );

        assertEquals("Admin user inactive", exception.getMessage());
    }

    @Test
    void getById_returns_blocked_time_for_admin() {
        User admin = createAdmin();
        Hall hall = createPartHallA();
        BlockedTime blockedTime = createBlockedTime(
                hall,
                admin,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0),
                BlockedTimeType.MANUAL
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(blockedTimeRepository.findById(blockedTime.getId())).thenReturn(Optional.of(blockedTime));

        BlockedTime result = service.getById(admin.getId(), blockedTime.getId());

        assertEquals(blockedTime.getId(), result.getId());
    }

    @Test
    void getById_rejects_unknown_blocked_time() {
        User admin = createAdmin();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(blockedTimeRepository.findById(any())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.getById(admin.getId(), UUID.randomUUID())
        );

        assertEquals("Blocked time not found", exception.getMessage());
    }

    @Test
    void getById_rejects_non_admin() {
        User representative = createRepresentative();

        when(userRepository.findById(representative.getId())).thenReturn(Optional.of(representative));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.getById(representative.getId(), UUID.randomUUID())
        );

        assertEquals("User not allowed to view blocked times", exception.getMessage());
    }

    @Test
    void getById_rejects_inactive_admin() {
        User inactiveAdmin = createInactiveAdmin();

        when(userRepository.findById(inactiveAdmin.getId())).thenReturn(Optional.of(inactiveAdmin));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.getById(inactiveAdmin.getId(), UUID.randomUUID())
        );

        assertEquals("Admin user inactive", exception.getMessage());
    }

    @Test
    void delete_success_for_admin() {
        User admin = createAdmin();
        Hall hall = createPartHallA();
        BlockedTime blockedTime = createBlockedTime(
                hall,
                admin,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 20, 11, 0),
                BlockedTimeType.MANUAL
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(blockedTimeRepository.findById(blockedTime.getId())).thenReturn(Optional.of(blockedTime));

        assertDoesNotThrow(() -> service.delete(blockedTime.getId(), admin.getId()));

        verify(blockedTimeRepository).deleteById(blockedTime.getId());
    }

    @Test
    void delete_rejects_unknown_admin() {
        UUID adminId = UUID.randomUUID();

        when(userRepository.findById(adminId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.delete(UUID.randomUUID(), adminId)
        );

        assertEquals("Admin user not found", exception.getMessage());
    }

    @Test
    void delete_rejects_non_admin() {
        User representative = createRepresentative();

        when(userRepository.findById(representative.getId())).thenReturn(Optional.of(representative));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.delete(UUID.randomUUID(), representative.getId())
        );

        assertEquals("User not allowed to delete blocked times", exception.getMessage());
    }

    @Test
    void delete_rejects_inactive_admin() {
        User inactiveAdmin = createInactiveAdmin();

        when(userRepository.findById(inactiveAdmin.getId())).thenReturn(Optional.of(inactiveAdmin));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.delete(UUID.randomUUID(), inactiveAdmin.getId())
        );

        assertEquals("Admin user inactive", exception.getMessage());
    }

    @Test
    void delete_rejects_unknown_blocked_time() {
        User admin = createAdmin();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(blockedTimeRepository.findById(any())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.delete(UUID.randomUUID(), admin.getId())
        );

        assertEquals("Blocked time not found", exception.getMessage());
        verify(blockedTimeRepository, never()).deleteById(any());
    }

    @Test
    void syncPublicHolidaysForYear_creates_public_holidays_for_full_hall_using_active_admin() {
        User admin = createAdmin();
        Hall fullHall = createFullHall();
        LocalDate holiday = LocalDate.of(2026, 12, 25);

        when(hallRepository.findAllActive()).thenReturn(List.of(fullHall));
        when(userRepository.findAllActive()).thenReturn(List.of(admin));
        when(publicHolidayPort.findHolidays(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                .thenReturn(List.of(holiday));
        when(blockedTimeRepository.findByHallIdAndTimeRange(
                eq(fullHall.getId()),
                eq(holiday.atStartOfDay()),
                eq(holiday.plusDays(1).atStartOfDay())
        )).thenReturn(List.of());
        when(blockedTimeRepository.save(any(BlockedTime.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.syncPublicHolidaysForYear(2026);

        verify(blockedTimeRepository).save(any(BlockedTime.class));
    }

    @Test
    void syncPublicHolidaysForYear_skips_when_no_full_hall_exists() {
        when(hallRepository.findAllActive()).thenReturn(List.of(createPartHallA()));

        assertDoesNotThrow(() -> service.syncPublicHolidaysForYear(2026));

        verify(publicHolidayPort, never()).findHolidays(any(), any());
        verify(blockedTimeRepository, never()).save(any());
    }

    @Test
    void syncPublicHolidaysForYear_skips_when_no_active_admin_exists() {
        Hall fullHall = createFullHall();

        when(hallRepository.findAllActive()).thenReturn(List.of(fullHall));
        when(userRepository.findAllActive()).thenReturn(List.of(createRepresentative()));

        assertDoesNotThrow(() -> service.syncPublicHolidaysForYear(2026));

        verify(publicHolidayPort, never()).findHolidays(any(), any());
        verify(blockedTimeRepository, never()).save(any());
    }

    @Test
    void syncPublicHolidaysForYear_does_not_duplicate_existing_public_holiday() {
        User admin = createAdmin();
        Hall fullHall = createFullHall();
        LocalDate holiday = LocalDate.of(2026, 12, 25);
        BlockedTime existingHoliday = createBlockedTime(
                fullHall,
                admin,
                holiday.atStartOfDay(),
                holiday.plusDays(1).atStartOfDay(),
                BlockedTimeType.PUBLIC_HOLIDAY
        );

        when(hallRepository.findAllActive()).thenReturn(List.of(fullHall));
        when(userRepository.findAllActive()).thenReturn(List.of(admin));
        when(publicHolidayPort.findHolidays(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                .thenReturn(List.of(holiday));
        when(blockedTimeRepository.findByHallIdAndTimeRange(
                eq(fullHall.getId()),
                eq(holiday.atStartOfDay()),
                eq(holiday.plusDays(1).atStartOfDay())
        )).thenReturn(List.of(existingHoliday));

        service.syncPublicHolidaysForYear(2026);

        verify(blockedTimeRepository, never()).save(any());
    }

    @Test
    void syncPublicHolidaysForYear_handles_holiday_api_exception_without_propagating() {
        User admin = createAdmin();
        Hall fullHall = createFullHall();

        when(hallRepository.findAllActive()).thenReturn(List.of(fullHall));
        when(userRepository.findAllActive()).thenReturn(List.of(admin));
        when(publicHolidayPort.findHolidays(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                .thenThrow(new RuntimeException("upstream down"));

        assertDoesNotThrow(() -> service.syncPublicHolidaysForYear(2026));
    }

    @Test
    void init_syncs_current_and_next_year() {
        User admin = createAdmin();
        Hall fullHall = createFullHall();

        when(hallRepository.findAllActive()).thenReturn(List.of(fullHall));
        when(userRepository.findAllActive()).thenReturn(List.of(admin));
        when(publicHolidayPort.findHolidays(any(), any())).thenReturn(List.of());

        service.init();

        verify(publicHolidayPort).findHolidays(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        verify(publicHolidayPort).findHolidays(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31));
    }
}