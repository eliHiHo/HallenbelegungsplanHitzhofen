package de.hallenbelegung.application.domain.service;

import de.hallenbelegung.application.domain.exception.ForbiddenException;
import de.hallenbelegung.application.domain.exception.NotFoundException;
import de.hallenbelegung.application.domain.exception.ValidationException;
import de.hallenbelegung.application.domain.model.Booking;
import de.hallenbelegung.application.domain.model.BookingSeries;
import de.hallenbelegung.application.domain.model.Hall;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.in.GetHallStatisticsUseCase;
import de.hallenbelegung.application.domain.port.in.GetSeriesStatisticsDetailUseCase;
import de.hallenbelegung.application.domain.port.in.GetSeriesStatisticsOverviewUseCase;
import de.hallenbelegung.application.domain.port.out.BookingRepositoryPort;
import de.hallenbelegung.application.domain.port.out.BookingSeriesRepositoryPort;
import de.hallenbelegung.application.domain.port.out.HallRepositoryPort;
import de.hallenbelegung.application.domain.port.out.UserRepositoryPort;
import de.hallenbelegung.application.domain.view.HallStatisticsView;
import de.hallenbelegung.application.domain.view.SeriesOccurrenceStatisticsView;
import de.hallenbelegung.application.domain.view.SeriesStatisticsDetailView;
import de.hallenbelegung.application.domain.view.SeriesStatisticsOverviewView;
import de.hallenbelegung.application.domain.view.SeriesUsageView;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import de.hallenbelegung.application.domain.port.out.HallConfigPort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
@Transactional
public class StatisticsService implements
        GetHallStatisticsUseCase,
        GetSeriesStatisticsOverviewUseCase,
        GetSeriesStatisticsDetailUseCase {

    private final BookingRepositoryPort bookingRepository;
    private final BookingSeriesRepositoryPort bookingSeriesRepository;
    private final HallRepositoryPort hallRepository;
    private final UserRepositoryPort userRepository;
    private final HallConfigPort config;

    public StatisticsService(BookingRepositoryPort bookingRepository,
                             BookingSeriesRepositoryPort bookingSeriesRepository,
                             HallRepositoryPort hallRepository,
                             UserRepositoryPort userRepository,
                             HallConfigPort config) {
        this.bookingRepository = bookingRepository;
        this.bookingSeriesRepository = bookingSeriesRepository;
        this.hallRepository = hallRepository;
        this.userRepository = userRepository;
        this.config = config;
    }

    @Override
    public List<HallStatisticsView> getHallStatistics(UUID currentUserId, LocalDate from, LocalDate to) {
        User user = loadActiveUser(currentUserId);
        validateDateRange(from, to);

        if (!user.isAdmin()) {
            throw new ForbiddenException("User not allowed to view hall statistics");
        }

        LocalDateTime rangeStart = from.atStartOfDay();
        LocalDateTime rangeEndExclusive = to.plusDays(1).atStartOfDay();

        return hallRepository.findAllActive()
                .stream()
                .sorted(Comparator.comparing(Hall::getId, Comparator.nullsLast(UUID::compareTo)))
                .map(hall -> buildHallStatistics(hall, rangeStart, rangeEndExclusive, from, to))
                .toList();
    }

    @Override
    public List<SeriesStatisticsOverviewView> getSeriesStatisticsOverview(UUID currentUserId, LocalDate from, LocalDate to) {
        User user = loadActiveUser(currentUserId);
        validateDateRange(from, to);

        List<BookingSeries> accessibleSeries = user.isAdmin()
                ? bookingSeriesRepository.findAll()
                : bookingSeriesRepository.findByResponsibleUserId(user.getId());

        return accessibleSeries.stream()
                .filter(series -> overlaps(series, from, to))
                .sorted(Comparator.comparing(BookingSeries::getCreatedAt).reversed())
                .map(series -> buildSeriesOverview(series, from, to))
                .toList();
    }

    @Override
    public SeriesStatisticsDetailView getSeriesStatisticsDetail(UUID currentUserId, UUID bookingSeriesId, LocalDate from, LocalDate to) {
        User user = loadActiveUser(currentUserId);
        validateDateRange(from, to);

        BookingSeries series = bookingSeriesRepository.findById(bookingSeriesId)
                .orElseThrow(() -> new NotFoundException("Booking series not found"));

        if (!user.isAdmin() && !series.getResponsibleUser().getId().equals(user.getId())) {
            throw new ForbiddenException("User not allowed to view this series statistics");
        }

        List<Booking> bookings = getBookingsForSeriesInRange(series.getId(), from, to);

        long totalAppointments = bookings.size();
        long cancelledAppointments = bookings.stream().filter(Booking::isCancelled).count();
        long conductedAppointments = bookings.stream()
                .filter(booking -> !booking.isCancelled())
                .filter(Booking::isConducted)
                .count();

        List<Booking> participantRelevantBookings = bookings.stream()
                .filter(booking -> !booking.isCancelled())
                .filter(booking -> booking.getParticipantCount() != null)
                .toList();

        long totalParticipants = participantRelevantBookings.stream()
                .mapToLong(booking -> booking.getParticipantCount().longValue())
                .sum();

        double averageParticipants = participantRelevantBookings.isEmpty()
                ? 0.0
                : (double) totalParticipants / participantRelevantBookings.size();

        List<SeriesOccurrenceStatisticsView> occurrences = bookings.stream()
                .sorted(Comparator.comparing(Booking::getStartAt))
                .map(booking -> new SeriesOccurrenceStatisticsView(
                        booking.getId(),
                        booking.getStartAt(),
                        booking.getEndAt(),
                        booking.isCancelled(),
                        booking.isConducted(),
                        booking.getParticipantCount(),
                        booking.getFeedbackComment()
                ))
                .toList();

        return new SeriesStatisticsDetailView(
                series.getId(),
                series.getTitle(),
                series.getHall().getName(),
                series.getResponsibleUser().getFullName(),
                totalAppointments,
                conductedAppointments,
                cancelledAppointments,
                totalParticipants,
                averageParticipants,
                occurrences
        );
    }

    private HallStatisticsView buildHallStatistics(Hall hall,
                                                   LocalDateTime rangeStart,
                                                   LocalDateTime rangeEndExclusive,
                                                   LocalDate from,
                                                   LocalDate to) {

        List<Booking> bookings = bookingRepository.findByHallIdAndTimeRange(hall.getId(), rangeStart, rangeEndExclusive)
                .stream()
                .filter(booking -> isWithinInclusiveDateRange(booking, from, to))
                .toList();

        long totalBookings = bookings.size();
        long cancelledBookings = bookings.stream().filter(Booking::isCancelled).count();

        long totalParticipants = bookings.stream()
                .filter(booking -> !booking.isCancelled())
                .filter(booking -> booking.getParticipantCount() != null)
                .mapToLong(booking -> booking.getParticipantCount().longValue())
                .sum();

        long bookedMinutes = bookings.stream()
                .filter(booking -> !booking.isCancelled())
                .mapToLong(booking -> ChronoUnit.MINUTES.between(
                        booking.getStartAt(),
                        booking.getEndAt()
                ))
                .sum();

        long totalAvailableMinutes = calculateTotalAvailableMinutes(from, to);
        double utilizationPercent = totalAvailableMinutes == 0
                ? 0.0
                : ((double) bookedMinutes / totalAvailableMinutes) * 100.0;

        Map<UUID, Long> seriesUsageMap = bookings.stream()
                .filter(booking -> !booking.isCancelled())
                .filter(Booking::belongsToSeries)
                .collect(Collectors.groupingBy(
                        booking -> booking.getBookingSeries().getId(),
                        Collectors.counting()
                ));

        Map<UUID, String> seriesTitleMap = bookings.stream()
                .filter(booking -> booking.getBookingSeries() != null)
                .collect(Collectors.toMap(
                        booking -> booking.getBookingSeries().getId(),
                        booking -> booking.getBookingSeries().getTitle(),
                        (left, right) -> left
                ));

        List<SeriesUsageView> topSeries = seriesUsageMap.entrySet()
                .stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> new SeriesUsageView(
                        entry.getKey(),
                        seriesTitleMap.get(entry.getKey()),
                        entry.getValue()
                ))
                .toList();

        return new HallStatisticsView(
                hall.getId(),
                hall.getName(),
                totalBookings,
                cancelledBookings,
                totalParticipants,
                utilizationPercent,
                topSeries
        );
    }

    private SeriesStatisticsOverviewView buildSeriesOverview(BookingSeries series, LocalDate from, LocalDate to) {
        List<Booking> bookings = getBookingsForSeriesInRange(series.getId(), from, to);

        long totalAppointments = bookings.size();
        long cancelledAppointments = bookings.stream().filter(Booking::isCancelled).count();
        long conductedAppointments = bookings.stream()
                .filter(booking -> !booking.isCancelled())
                .filter(Booking::isConducted)
                .count();

        List<Booking> participantRelevantBookings = bookings.stream()
                .filter(booking -> !booking.isCancelled())
                .filter(booking -> booking.getParticipantCount() != null)
                .toList();

        long totalParticipants = participantRelevantBookings.stream()
                .mapToLong(booking -> booking.getParticipantCount().longValue())
                .sum();

        double averageParticipants = participantRelevantBookings.isEmpty()
                ? 0.0
                : (double) totalParticipants / participantRelevantBookings.size();

        return new SeriesStatisticsOverviewView(
                series.getId(),
                series.getTitle(),
                series.getHall().getName(),
                totalAppointments,
                conductedAppointments,
                cancelledAppointments,
                totalParticipants,
                averageParticipants
        );
    }

    private List<Booking> getBookingsForSeriesInRange(UUID bookingSeriesId, LocalDate from, LocalDate to) {
        return bookingRepository.findByBookingSeriesId(bookingSeriesId)
                .stream()
                .filter(booking -> isWithinInclusiveDateRange(booking, from, to))
                .toList();
    }

    private boolean isWithinInclusiveDateRange(Booking booking, LocalDate from, LocalDate to) {
        return !booking.getStartAt().toLocalDate().isBefore(from) && !booking.getStartAt().toLocalDate().isAfter(to);
    }

    private boolean overlaps(BookingSeries series, LocalDate from, LocalDate to) {
        return !series.getEndDate().isBefore(from) && !series.getStartDate().isAfter(to);
    }

    private long calculateTotalAvailableMinutes(LocalDate from, LocalDate to) {
        long days = ChronoUnit.DAYS.between(from, to.plusDays(1));
        long openMinutesPerDay = ChronoUnit.MINUTES.between(config.openingStart(), config.openingEnd());
        return days * openMinutesPerDay;
    }

    private User loadActiveUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!user.isActive()) {
            throw new ForbiddenException("User inactive");
        }

        return user;
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new ValidationException("From and to date are required");
        }

        if (from.isAfter(to)) {
            throw new ValidationException("From date must be before or equal to to date");
        }
    }
}