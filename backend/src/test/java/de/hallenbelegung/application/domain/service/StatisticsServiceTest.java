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
import de.hallenbelegung.application.domain.port.out.HallConfigPort;
import de.hallenbelegung.application.domain.port.out.HallRepositoryPort;
import de.hallenbelegung.application.domain.port.out.UserRepositoryPort;
import de.hallenbelegung.application.domain.view.HallStatisticsView;
import de.hallenbelegung.application.domain.view.SeriesOccurrenceStatisticsView;
import de.hallenbelegung.application.domain.view.SeriesStatisticsDetailView;
import de.hallenbelegung.application.domain.view.SeriesStatisticsOverviewView;
import de.hallenbelegung.application.domain.view.SeriesUsageView;
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

public class StatisticsServiceTest {

    private BookingRepositoryPort bookingRepository;
    private BookingSeriesRepositoryPort bookingSeriesRepository;
    private HallRepositoryPort hallRepository;
    private UserRepositoryPort userRepository;
    private HallConfigPort config;

    private StatisticsService service;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepositoryPort.class);
        bookingSeriesRepository = mock(BookingSeriesRepositoryPort.class);
        hallRepository = mock(HallRepositoryPort.class);
        userRepository = mock(UserRepositoryPort.class);
        config = mock(HallConfigPort.class);

        when(config.openingStart()).thenReturn(LocalTime.of(8, 0));
        when(config.openingEnd()).thenReturn(LocalTime.of(22, 0));

        service = new StatisticsService(
                bookingRepository,
                bookingSeriesRepository,
                hallRepository,
                userRepository,
                config
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

    private Hall createHallA() {
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

    private Hall createHallB() {
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

    private BookingSeries createSeries(User owner, Hall hall, String title) {
        Instant now = Instant.now();
        return new BookingSeries(
                UUID.randomUUID(),
                title,
                "Serientraining",
                DayOfWeek.MONDAY,
                LocalTime.of(18, 0),
                LocalTime.of(19, 30),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
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

    private Booking createBooking(User owner,
                                  Hall hall,
                                  BookingSeries series,
                                  LocalDateTime start,
                                  LocalDateTime end,
                                  Integer participantCount,
                                  boolean conducted,
                                  boolean cancelled) {
        Instant now = Instant.now();

        Booking booking = new Booking(
                UUID.randomUUID(),
                series != null ? series.getTitle() : "Einzeltermin",
                "Beschreibung",
                start,
                end,
                BookingStatus.APPROVED,
                participantCount,
                conducted,
                participantCount != null ? "Feedback" : null,
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

        if (cancelled) {
            booking.cancel(owner, "Ausfall");
        }

        return booking;
    }

    @Test
    void getHallStatistics_allows_admin() {
        User admin = createAdmin();
        Hall hallA = createHallA();
        Hall hallB = createHallB();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(hallRepository.findAllActive()).thenReturn(List.of(hallA, hallB));
        when(bookingRepository.findByHallIdAndTimeRange(any(), any(), any())).thenReturn(List.of());

        List<HallStatisticsView> result = service.getHallStatistics(
                admin.getId(),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        );

        assertEquals(2, result.size());
    }

    @Test
    void getHallStatistics_rejects_non_admin() {
        User representative = createRepresentative();

        when(userRepository.findById(representative.getId())).thenReturn(Optional.of(representative));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.getHallStatistics(
                        representative.getId(),
                        LocalDate.of(2026, 4, 1),
                        LocalDate.of(2026, 4, 30)
                )
        );

        assertEquals("User not allowed to view hall statistics", exception.getMessage());
    }

    @Test
    void getHallStatistics_rejects_unknown_user() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.getHallStatistics(
                        userId,
                        LocalDate.of(2026, 4, 1),
                        LocalDate.of(2026, 4, 30)
                )
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void getHallStatistics_rejects_inactive_user() {
        User inactive = createInactiveRepresentative();

        when(userRepository.findById(inactive.getId())).thenReturn(Optional.of(inactive));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.getHallStatistics(
                        inactive.getId(),
                        LocalDate.of(2026, 4, 1),
                        LocalDate.of(2026, 4, 30)
                )
        );

        assertEquals("User inactive", exception.getMessage());
    }

    @Test
    void getHallStatistics_rejects_null_from() {
        User admin = createAdmin();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.getHallStatistics(admin.getId(), null, LocalDate.of(2026, 4, 30))
        );

        assertEquals("From and to date are required", exception.getMessage());
    }

    @Test
    void getHallStatistics_rejects_invalid_date_range() {
        User admin = createAdmin();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.getHallStatistics(
                        admin.getId(),
                        LocalDate.of(2026, 4, 30),
                        LocalDate.of(2026, 4, 1)
                )
        );

        assertEquals("From date must be before or equal to to date", exception.getMessage());
    }

    @Test
    void getHallStatistics_counts_total_cancelled_participants_and_utilization_correctly() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hallA = createHallA();

        BookingSeries series = createSeries(owner, hallA, "Volleyball");

        Booking booking1 = createBooking(
                owner,
                hallA,
                series,
                LocalDateTime.of(2026, 4, 7, 18, 0),
                LocalDateTime.of(2026, 4, 7, 19, 0),
                20,
                true,
                false
        );
        Booking booking2 = createBooking(
                owner,
                hallA,
                series,
                LocalDateTime.of(2026, 4, 14, 18, 0),
                LocalDateTime.of(2026, 4, 14, 19, 30),
                null,
                false,
                false
        );
        Booking booking3 = createBooking(
                owner,
                hallA,
                series,
                LocalDateTime.of(2026, 4, 21, 18, 0),
                LocalDateTime.of(2026, 4, 21, 19, 0),
                30,
                true,
                true
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(hallRepository.findAllActive()).thenReturn(List.of(hallA));
        when(bookingRepository.findByHallIdAndTimeRange(eq(hallA.getId()), any(), any()))
                .thenReturn(List.of(booking1, booking2, booking3));

        List<HallStatisticsView> result = service.getHallStatistics(
                admin.getId(),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        );

        HallStatisticsView stats = result.get(0);

        assertEquals(hallA.getId(), stats.getHallId());
        assertEquals(3, stats.getTotalBookings());
        assertEquals(1, stats.getCancelledBookings());
        assertEquals(20, stats.getTotalParticipants());
        assertEquals((150.0 / 25200.0) * 100.0, stats.getUtilizationPercent(), 0.0000001);
    }

    @Test
    void getHallStatistics_builds_top_series_usage() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hallA = createHallA();

        BookingSeries series1 = createSeries(owner, hallA, "Volleyball");
        BookingSeries series2 = createSeries(owner, hallA, "Basketball");

        Booking s1b1 = createBooking(
                owner, hallA, series1,
                LocalDateTime.of(2026, 4, 7, 18, 0),
                LocalDateTime.of(2026, 4, 7, 19, 0),
                10, true, false
        );
        Booking s1b2 = createBooking(
                owner, hallA, series1,
                LocalDateTime.of(2026, 4, 14, 18, 0),
                LocalDateTime.of(2026, 4, 14, 19, 0),
                12, true, false
        );
        Booking s2b1 = createBooking(
                owner, hallA, series2,
                LocalDateTime.of(2026, 4, 9, 18, 0),
                LocalDateTime.of(2026, 4, 9, 19, 0),
                8, true, false
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(hallRepository.findAllActive()).thenReturn(List.of(hallA));
        when(bookingRepository.findByHallIdAndTimeRange(eq(hallA.getId()), any(), any()))
                .thenReturn(List.of(s1b1, s1b2, s2b1));

        List<HallStatisticsView> result = service.getHallStatistics(
                admin.getId(),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        );

        List<SeriesUsageView> topSeries = result.get(0).getTopSeries();

        assertEquals(2, topSeries.size());
        assertEquals(series1.getId(), topSeries.get(0).getBookingSeriesId());
        assertEquals("Volleyball", topSeries.get(0).getTitle());
        assertEquals(2L, topSeries.get(0).getBookingCount());
    }

    @Test
    void getHallStatistics_limits_top_series_to_five_and_ignores_cancelled_bookings() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hallA = createHallA();

        BookingSeries series1 = createSeries(owner, hallA, "Serie 1");
        BookingSeries series2 = createSeries(owner, hallA, "Serie 2");
        BookingSeries series3 = createSeries(owner, hallA, "Serie 3");
        BookingSeries series4 = createSeries(owner, hallA, "Serie 4");
        BookingSeries series5 = createSeries(owner, hallA, "Serie 5");
        BookingSeries series6 = createSeries(owner, hallA, "Serie 6");

        LocalDateTime start = LocalDateTime.of(2026, 4, 7, 18, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 7, 19, 0);

        Booking booking1 = createBooking(owner, hallA, series1, start, end, 10, true, false);
        Booking cancelledBooking1 = createBooking(owner, hallA, series1, start.plusDays(7), end.plusDays(7), 99, true, true);
        Booking booking2 = createBooking(owner, hallA, series2, start.plusDays(1), end.plusDays(1), 11, true, false);
        Booking booking3 = createBooking(owner, hallA, series3, start.plusDays(2), end.plusDays(2), 12, true, false);
        Booking booking4 = createBooking(owner, hallA, series4, start.plusDays(3), end.plusDays(3), 13, true, false);
        Booking booking5 = createBooking(owner, hallA, series5, start.plusDays(4), end.plusDays(4), 14, true, false);
        Booking booking6 = createBooking(owner, hallA, series6, start.plusDays(5), end.plusDays(5), 15, true, false);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(hallRepository.findAllActive()).thenReturn(List.of(hallA));
        when(bookingRepository.findByHallIdAndTimeRange(eq(hallA.getId()), any(), any()))
                .thenReturn(List.of(
                        booking1,
                        cancelledBooking1,
                        booking2,
                        booking3,
                        booking4,
                        booking5,
                        booking6
                ));

        List<HallStatisticsView> result = service.getHallStatistics(
                admin.getId(),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        );

        List<SeriesUsageView> topSeries = result.get(0).getTopSeries();

        assertEquals(5, topSeries.size());
    }

    @Test
    void getHallStatistics_ignores_cancelled_bookings_in_series_usage() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hallA = createHallA();

        BookingSeries series = createSeries(owner, hallA, "Volleyball");

        Booking activeBooking = createBooking(
                owner,
                hallA,
                series,
                LocalDateTime.of(2026, 4, 7, 18, 0),
                LocalDateTime.of(2026, 4, 7, 19, 0),
                10,
                true,
                false
        );
        Booking cancelledBooking = createBooking(
                owner,
                hallA,
                series,
                LocalDateTime.of(2026, 4, 14, 18, 0),
                LocalDateTime.of(2026, 4, 14, 19, 0),
                12,
                true,
                true
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(hallRepository.findAllActive()).thenReturn(List.of(hallA));
        when(bookingRepository.findByHallIdAndTimeRange(eq(hallA.getId()), any(), any()))
                .thenReturn(List.of(activeBooking, cancelledBooking));

        List<HallStatisticsView> result = service.getHallStatistics(
                admin.getId(),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        );

        List<SeriesUsageView> topSeries = result.getFirst().getTopSeries();

        assertEquals(1, topSeries.size());
        assertEquals(series.getId(), topSeries.getFirst().getBookingSeriesId());
        assertEquals(1L, topSeries.getFirst().getBookingCount());
    }

    @Test
    void getSeriesStatisticsDetail_includes_boundary_bookings_in_range() {
        User owner = createRepresentative();
        Hall hall = createHallA();
        BookingSeries series = createSeries(owner, hall, "Volleyball");

        Booking fromBoundary = createBooking(
                owner,
                hall,
                series,
                LocalDateTime.of(2026, 4, 1, 18, 0),
                LocalDateTime.of(2026, 4, 1, 19, 0),
                20,
                true,
                false
        );
        Booking toBoundary = createBooking(
                owner,
                hall,
                series,
                LocalDateTime.of(2026, 4, 30, 18, 0),
                LocalDateTime.of(2026, 4, 30, 19, 0),
                18,
                true,
                false
        );
        Booking outsideRange = createBooking(
                owner,
                hall,
                series,
                LocalDateTime.of(2026, 5, 1, 18, 0),
                LocalDateTime.of(2026, 5, 1, 19, 0),
                22,
                true,
                false
        );

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingSeriesRepository.findById(series.getId())).thenReturn(Optional.of(series));
        when(bookingRepository.findByBookingSeriesId(series.getId()))
                .thenReturn(List.of(fromBoundary, toBoundary, outsideRange));

        SeriesStatisticsDetailView result = service.getSeriesStatisticsDetail(
                owner.getId(),
                series.getId(),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        );

        assertEquals(2, result.getTotalAppointments());
        assertEquals(2, result.getOccurrences().size());
        assertEquals(fromBoundary.getId(), result.getOccurrences().get(0).getBookingId());
        assertEquals(toBoundary.getId(), result.getOccurrences().get(1).getBookingId());
    }

    @Test
    void getSeriesStatisticsOverview_for_admin_returns_all_overlapping_series_sorted_by_created_at_desc() {
        User admin = createAdmin();
        User ownerA = createRepresentative();
        User ownerB = createOtherRepresentative();
        Hall hall = createHallA();

        Instant olderInstant = Instant.parse("2026-04-01T10:00:00Z");
        Instant newerInstant = Instant.parse("2026-04-05T10:00:00Z");

        BookingSeries older = new BookingSeries(
                UUID.randomUUID(),
                "Alt",
                "Serie alt",
                DayOfWeek.MONDAY,
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                BookingSeriesStatus.ACTIVE,
                hall,
                ownerA,
                ownerA,
                ownerA,
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
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                BookingSeriesStatus.ACTIVE,
                hall,
                ownerB,
                ownerB,
                ownerB,
                null,
                newerInstant,
                newerInstant,
                null,
                null
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingSeriesRepository.findAll()).thenReturn(List.of(older, newer));
        when(bookingRepository.findByBookingSeriesId(older.getId())).thenReturn(List.of());
        when(bookingRepository.findByBookingSeriesId(newer.getId())).thenReturn(List.of());

        List<SeriesStatisticsOverviewView> result = service.getSeriesStatisticsOverview(
                admin.getId(),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        );

        assertEquals(2, result.size());
        assertEquals(newer.getId(), result.get(0).getBookingSeriesId());
        assertEquals(older.getId(), result.get(1).getBookingSeriesId());
    }

    @Test
    void getSeriesStatisticsOverview_for_representative_returns_only_own_series() {
        User representative = createRepresentative();
        Hall hall = createHallA();

        BookingSeries ownSeries1 = createSeries(representative, hall, "Volleyball");
        BookingSeries ownSeries2 = createSeries(representative, hall, "Basketball");

        when(userRepository.findById(representative.getId())).thenReturn(Optional.of(representative));
        when(bookingSeriesRepository.findByResponsibleUserId(representative.getId()))
                .thenReturn(List.of(ownSeries1, ownSeries2));
        when(bookingRepository.findByBookingSeriesId(ownSeries1.getId())).thenReturn(List.of());
        when(bookingRepository.findByBookingSeriesId(ownSeries2.getId())).thenReturn(List.of());

        List<SeriesStatisticsOverviewView> result = service.getSeriesStatisticsOverview(
                representative.getId(),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        );

        assertEquals(2, result.size());
    }

    @Test
    void getSeriesStatisticsOverview_rejects_unknown_user() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.getSeriesStatisticsOverview(
                        userId,
                        LocalDate.of(2026, 4, 1),
                        LocalDate.of(2026, 4, 30)
                )
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void getSeriesStatisticsOverview_rejects_inactive_user() {
        User inactive = createInactiveRepresentative();

        when(userRepository.findById(inactive.getId())).thenReturn(Optional.of(inactive));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.getSeriesStatisticsOverview(
                        inactive.getId(),
                        LocalDate.of(2026, 4, 1),
                        LocalDate.of(2026, 4, 30)
                )
        );

        assertEquals("User inactive", exception.getMessage());
    }

    @Test
    void getSeriesStatisticsOverview_rejects_null_date_range() {
        User admin = createAdmin();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingSeriesRepository.findAll()).thenReturn(List.of());

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.getSeriesStatisticsOverview(admin.getId(), null, LocalDate.of(2026, 4, 30))
        );

        assertEquals("From and to date are required", exception.getMessage());
    }

    @Test
    void getSeriesStatisticsOverview_filters_non_overlapping_series() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createHallA();

        BookingSeries aprilSeries = new BookingSeries(
                UUID.randomUUID(),
                "April",
                "Serie April",
                DayOfWeek.MONDAY,
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
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

        BookingSeries maySeries = new BookingSeries(
                UUID.randomUUID(),
                "May",
                "Serie May",
                DayOfWeek.MONDAY,
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
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

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingSeriesRepository.findAll()).thenReturn(List.of(aprilSeries, maySeries));
        when(bookingRepository.findByBookingSeriesId(aprilSeries.getId())).thenReturn(List.of());

        List<SeriesStatisticsOverviewView> result = service.getSeriesStatisticsOverview(
                admin.getId(),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        );

        assertEquals(1, result.size());
        assertEquals(aprilSeries.getId(), result.get(0).getBookingSeriesId());
    }

    @Test
    void getSeriesStatisticsOverview_computes_counts_correctly() {
        User representative = createRepresentative();
        Hall hall = createHallA();
        BookingSeries series = createSeries(representative, hall, "Volleyball");

        Booking booking1 = createBooking(
                representative,
                hall,
                series,
                LocalDateTime.of(2026, 4, 7, 18, 0),
                LocalDateTime.of(2026, 4, 7, 19, 0),
                20,
                true,
                false
        );
        Booking booking2 = createBooking(
                representative,
                hall,
                series,
                LocalDateTime.of(2026, 4, 14, 18, 0),
                LocalDateTime.of(2026, 4, 14, 19, 0),
                16,
                true,
                false
        );
        Booking booking3 = createBooking(
                representative,
                hall,
                series,
                LocalDateTime.of(2026, 4, 21, 18, 0),
                LocalDateTime.of(2026, 4, 21, 19, 0),
                null,
                false,
                true
        );
        Booking booking4 = createBooking(
                representative,
                hall,
                series,
                LocalDateTime.of(2026, 5, 5, 18, 0),
                LocalDateTime.of(2026, 5, 5, 19, 0),
                30,
                true,
                false
        );

        when(userRepository.findById(representative.getId())).thenReturn(Optional.of(representative));
        when(bookingSeriesRepository.findByResponsibleUserId(representative.getId()))
                .thenReturn(List.of(series));
        when(bookingRepository.findByBookingSeriesId(series.getId()))
                .thenReturn(List.of(booking1, booking2, booking3, booking4));

        List<SeriesStatisticsOverviewView> result = service.getSeriesStatisticsOverview(
                representative.getId(),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        );

        SeriesStatisticsOverviewView item = result.get(0);

        assertEquals(series.getId(), item.getBookingSeriesId());
        assertEquals(3, item.getTotalAppointments());
        assertEquals(2, item.getConductedAppointments());
        assertEquals(1, item.getCancelledAppointments());
        assertEquals(36, item.getTotalParticipants());
        assertEquals(18.0, item.getAverageParticipants(), 0.000001);
    }

    @Test
    void getSeriesStatisticsOverview_returns_zero_average_when_no_participant_relevant_bookings() {
        User representative = createRepresentative();
        Hall hall = createHallA();
        BookingSeries series = createSeries(representative, hall, "Volleyball");

        Booking noFeedbackBooking = createBooking(
                representative,
                hall,
                series,
                LocalDateTime.of(2026, 4, 7, 18, 0),
                LocalDateTime.of(2026, 4, 7, 19, 0),
                null,
                true,
                false
        );
        Booking cancelledBooking = createBooking(
                representative,
                hall,
                series,
                LocalDateTime.of(2026, 4, 14, 18, 0),
                LocalDateTime.of(2026, 4, 14, 19, 0),
                30,
                false,
                true
        );

        when(userRepository.findById(representative.getId())).thenReturn(Optional.of(representative));
        when(bookingSeriesRepository.findByResponsibleUserId(representative.getId())).thenReturn(List.of(series));
        when(bookingRepository.findByBookingSeriesId(series.getId())).thenReturn(List.of(noFeedbackBooking, cancelledBooking));

        List<SeriesStatisticsOverviewView> result = service.getSeriesStatisticsOverview(
                representative.getId(),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        );

        SeriesStatisticsOverviewView item = result.get(0);
        assertEquals(0, item.getTotalParticipants());
        assertEquals(0.0, item.getAverageParticipants(), 0.000001);
    }

    @Test
    void getSeriesStatisticsDetail_allows_admin() {
        User admin = createAdmin();
        User owner = createRepresentative();
        Hall hall = createHallA();
        BookingSeries series = createSeries(owner, hall, "Volleyball");

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(bookingSeriesRepository.findById(series.getId())).thenReturn(Optional.of(series));
        when(bookingRepository.findByBookingSeriesId(series.getId())).thenReturn(List.of());

        SeriesStatisticsDetailView result = service.getSeriesStatisticsDetail(
                admin.getId(),
                series.getId(),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        );

        assertEquals(series.getId(), result.getBookingSeriesId());
    }

    @Test
    void getSeriesStatisticsDetail_allows_owner() {
        User owner = createRepresentative();
        Hall hall = createHallA();
        BookingSeries series = createSeries(owner, hall, "Volleyball");

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingSeriesRepository.findById(series.getId())).thenReturn(Optional.of(series));
        when(bookingRepository.findByBookingSeriesId(series.getId())).thenReturn(List.of());

        SeriesStatisticsDetailView result = service.getSeriesStatisticsDetail(
                owner.getId(),
                series.getId(),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        );

        assertEquals(series.getId(), result.getBookingSeriesId());
    }

    @Test
    void getSeriesStatisticsDetail_rejects_other_representative() {
        User owner = createRepresentative();
        User other = createOtherRepresentative();
        Hall hall = createHallA();
        BookingSeries series = createSeries(owner, hall, "Volleyball");

        when(userRepository.findById(other.getId())).thenReturn(Optional.of(other));
        when(bookingSeriesRepository.findById(series.getId())).thenReturn(Optional.of(series));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.getSeriesStatisticsDetail(
                        other.getId(),
                        series.getId(),
                        LocalDate.of(2026, 4, 1),
                        LocalDate.of(2026, 4, 30)
                )
        );

        assertEquals("User not allowed to view this series statistics", exception.getMessage());
    }

    @Test
    void getSeriesStatisticsDetail_rejects_unknown_user() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.getSeriesStatisticsDetail(
                        userId,
                        UUID.randomUUID(),
                        LocalDate.of(2026, 4, 1),
                        LocalDate.of(2026, 4, 30)
                )
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void getSeriesStatisticsDetail_rejects_inactive_user() {
        User inactive = createInactiveRepresentative();

        when(userRepository.findById(inactive.getId())).thenReturn(Optional.of(inactive));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.getSeriesStatisticsDetail(
                        inactive.getId(),
                        UUID.randomUUID(),
                        LocalDate.of(2026, 4, 1),
                        LocalDate.of(2026, 4, 30)
                )
        );

        assertEquals("User inactive", exception.getMessage());
    }

    @Test
    void getSeriesStatisticsDetail_rejects_unknown_series() {
        User owner = createRepresentative();

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingSeriesRepository.findById(any())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.getSeriesStatisticsDetail(
                        owner.getId(),
                        UUID.randomUUID(),
                        LocalDate.of(2026, 4, 1),
                        LocalDate.of(2026, 4, 30)
                )
        );

        assertEquals("Booking series not found", exception.getMessage());
    }

    @Test
    void getSeriesStatisticsDetail_rejects_invalid_date_range() {
        User owner = createRepresentative();
        Hall hall = createHallA();
        BookingSeries series = createSeries(owner, hall, "Volleyball");

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.getSeriesStatisticsDetail(
                        owner.getId(),
                        series.getId(),
                        LocalDate.of(2026, 4, 30),
                        LocalDate.of(2026, 4, 1)
                )
        );

        assertEquals("From date must be before or equal to to date", exception.getMessage());
    }

    @Test
    void getSeriesStatisticsDetail_computes_counts_and_occurrences_correctly() {
        User owner = createRepresentative();
        Hall hall = createHallA();
        BookingSeries series = createSeries(owner, hall, "Volleyball");

        Booking booking1 = createBooking(
                owner,
                hall,
                series,
                LocalDateTime.of(2026, 4, 7, 18, 0),
                LocalDateTime.of(2026, 4, 7, 19, 0),
                20,
                true,
                false
        );
        Booking booking2 = createBooking(
                owner,
                hall,
                series,
                LocalDateTime.of(2026, 4, 14, 18, 0),
                LocalDateTime.of(2026, 4, 14, 19, 0),
                16,
                true,
                false
        );
        Booking booking3 = createBooking(
                owner,
                hall,
                series,
                LocalDateTime.of(2026, 4, 21, 18, 0),
                LocalDateTime.of(2026, 4, 21, 19, 0),
                null,
                false,
                true
        );
        Booking booking4 = createBooking(
                owner,
                hall,
                series,
                LocalDateTime.of(2026, 4, 28, 18, 0),
                LocalDateTime.of(2026, 4, 28, 19, 0),
                null,
                false,
                false
        );

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingSeriesRepository.findById(series.getId())).thenReturn(Optional.of(series));
        when(bookingRepository.findByBookingSeriesId(series.getId()))
                .thenReturn(List.of(booking1, booking2, booking3, booking4));

        SeriesStatisticsDetailView result = service.getSeriesStatisticsDetail(
                owner.getId(),
                series.getId(),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        );

        assertEquals(series.getId(), result.getBookingSeriesId());
        assertEquals("Volleyball", result.getTitle());
        assertEquals("Halle A", result.getHallName());
        assertEquals(owner.getFullName(), result.getResponsibleUserName());
        assertEquals(4, result.getTotalAppointments());
        assertEquals(2, result.getConductedAppointments());
        assertEquals(1, result.getCancelledAppointments());
        assertEquals(36, result.getTotalParticipants());
        assertEquals(18.0, result.getAverageParticipants(), 0.000001);
        assertEquals(4, result.getOccurrences().size());

        SeriesOccurrenceStatisticsView firstOccurrence = result.getOccurrences().get(0);
        assertEquals(booking1.getId(), firstOccurrence.getBookingId());
        assertEquals(booking1.getStartAt(), firstOccurrence.getStartDateTime());
        assertEquals(booking1.getEndAt(), firstOccurrence.getEndDateTime());
        assertFalse(firstOccurrence.isCancelled());
        assertTrue(firstOccurrence.isConducted());
        assertEquals(20, firstOccurrence.getParticipantCount());
        assertEquals("Feedback", firstOccurrence.getFeedbackComment());
    }

    @Test
    void getSeriesStatisticsDetail_filters_by_date_range() {
        User owner = createRepresentative();
        Hall hall = createHallA();
        BookingSeries series = createSeries(owner, hall, "Volleyball");

        Booking aprilBooking = createBooking(
                owner,
                hall,
                series,
                LocalDateTime.of(2026, 4, 7, 18, 0),
                LocalDateTime.of(2026, 4, 7, 19, 0),
                20,
                true,
                false
        );
        Booking mayBooking = createBooking(
                owner,
                hall,
                series,
                LocalDateTime.of(2026, 5, 5, 18, 0),
                LocalDateTime.of(2026, 5, 5, 19, 0),
                30,
                true,
                false
        );

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingSeriesRepository.findById(series.getId())).thenReturn(Optional.of(series));
        when(bookingRepository.findByBookingSeriesId(series.getId()))
                .thenReturn(List.of(aprilBooking, mayBooking));

        SeriesStatisticsDetailView result = service.getSeriesStatisticsDetail(
                owner.getId(),
                series.getId(),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        );

        assertEquals(1, result.getTotalAppointments());
        assertEquals(20, result.getTotalParticipants());
        assertEquals(1, result.getOccurrences().size());
    }

    @Test
    void getSeriesStatisticsDetail_ignores_missing_participant_feedback_for_average() {
        User owner = createRepresentative();
        Hall hall = createHallA();
        BookingSeries series = createSeries(owner, hall, "Volleyball");

        Booking withParticipants = createBooking(
                owner,
                hall,
                series,
                LocalDateTime.of(2026, 4, 7, 18, 0),
                LocalDateTime.of(2026, 4, 7, 19, 0),
                20,
                true,
                false
        );
        Booking withoutParticipants = createBooking(
                owner,
                hall,
                series,
                LocalDateTime.of(2026, 4, 14, 18, 0),
                LocalDateTime.of(2026, 4, 14, 19, 0),
                null,
                true,
                false
        );

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingSeriesRepository.findById(series.getId())).thenReturn(Optional.of(series));
        when(bookingRepository.findByBookingSeriesId(series.getId()))
                .thenReturn(List.of(withParticipants, withoutParticipants));

        SeriesStatisticsDetailView result = service.getSeriesStatisticsDetail(
                owner.getId(),
                series.getId(),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        );

        assertEquals(20, result.getTotalParticipants());
        assertEquals(20.0, result.getAverageParticipants(), 0.000001);
    }

    @Test
    void getSeriesStatisticsDetail_excludes_cancelled_bookings_from_participant_stats() {
        User owner = createRepresentative();
        Hall hall = createHallA();
        BookingSeries series = createSeries(owner, hall, "Volleyball");

        Booking normalBooking = createBooking(
                owner,
                hall,
                series,
                LocalDateTime.of(2026, 4, 7, 18, 0),
                LocalDateTime.of(2026, 4, 7, 19, 0),
                20,
                true,
                false
        );
        Booking cancelledBooking = createBooking(
                owner,
                hall,
                series,
                LocalDateTime.of(2026, 4, 14, 18, 0),
                LocalDateTime.of(2026, 4, 14, 19, 0),
                40,
                true,
                true
        );

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(bookingSeriesRepository.findById(series.getId())).thenReturn(Optional.of(series));
        when(bookingRepository.findByBookingSeriesId(series.getId()))
                .thenReturn(List.of(normalBooking, cancelledBooking));

        SeriesStatisticsDetailView result = service.getSeriesStatisticsDetail(
                owner.getId(),
                series.getId(),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        );

        assertEquals(2, result.getTotalAppointments());
        assertEquals(1, result.getCancelledAppointments());
        assertEquals(20, result.getTotalParticipants());
        assertEquals(20.0, result.getAverageParticipants(), 0.000001);
    }
}