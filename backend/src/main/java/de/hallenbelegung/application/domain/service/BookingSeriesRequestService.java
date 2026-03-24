package de.hallenbelegung.application.domain.service;

import de.hallenbelegung.application.domain.exception.ForbiddenException;
import de.hallenbelegung.application.domain.exception.NotFoundException;
import de.hallenbelegung.application.domain.exception.ValidationException;
import de.hallenbelegung.application.domain.model.Booking;
import de.hallenbelegung.application.domain.model.BookingRequestStatus;
import de.hallenbelegung.application.domain.model.BookingSeries;
import de.hallenbelegung.application.domain.model.BookingSeriesRequest;
import de.hallenbelegung.application.domain.model.Hall;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.in.*;
import de.hallenbelegung.application.domain.port.out.*;
import jakarta.transaction.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import de.hallenbelegung.application.domain.port.out.HallConfigPort;

@Transactional
public class BookingSeriesRequestService implements ApproveBookingSeriesRequestUseCase,
        RejectBookingSeriesRequestUseCase,
        CreateBookingSeriesRequestUseCase,
        GetBookingSeriesRequestUseCase,
        GetBookingSeriesRequestsUseCase {

    private final BookingSeriesRequestRepositoryPort bookingSeriesRequestRepository;
    private final BookingSeriesRepositoryPort bookingSeriesRepository;
    private final BookingRepositoryPort bookingRepository;
    private final BlockedTimeRepositoryPort blockedTimeRepository;
    private final UserRepositoryPort userRepository;
    private final HallRepositoryPort hallRepository;
    private final HallConfigPort config;
    private final Clock clock;
    private final NotificationPort notificationPort;

    public BookingSeriesRequestService(
            BookingSeriesRequestRepositoryPort bookingSeriesRequestRepository,
            BookingSeriesRepositoryPort bookingSeriesRepository,
            BookingRepositoryPort bookingRepository,
            BlockedTimeRepositoryPort blockedTimeRepository,
            UserRepositoryPort userRepository,
            HallRepositoryPort hallRepository,
            HallConfigPort config,
            Clock clock,
            NotificationPort notificationPort
    ) {
        this.bookingSeriesRequestRepository = bookingSeriesRequestRepository;
        this.bookingSeriesRepository = bookingSeriesRepository;
        this.bookingRepository = bookingRepository;
        this.blockedTimeRepository = blockedTimeRepository;
        this.userRepository = userRepository;
        this.hallRepository = hallRepository;
        this.config = config;
        this.clock = clock;
        this.notificationPort = notificationPort;
    }

    public Long create(Long userId,
                       Long hallId,
                       String title,
                       String description,
                       DayOfWeek weekday,
                       LocalTime startTime,
                       LocalTime endTime,
                       LocalDate startDate,
                       LocalDate endDate) {

        User user = loadActiveUser(userId);

        if (!user.isClubRepresentative() && !user.isAdmin()) {
            throw new ForbiddenException("User not allowed to create booking series request");
        }

        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new NotFoundException("Hall not found"));

        if (!hall.isActive()) {
            throw new ForbiddenException("Hall inactive");
        }

        validateCreateInput(title, weekday, startTime, endTime, startDate, endDate);
        validateTimeGrid(startTime, endTime);
        validateOpeningHours(startTime, endTime);

        List<LocalDate> occurrences = buildOccurrences(weekday, startDate, endDate);
        if (occurrences.isEmpty()) {
            throw new ValidationException("Series request does not contain any valid appointment");
        }

        BookingSeriesRequest request = BookingSeriesRequest.createNew(
                title,
                description,
                weekday,
                startTime,
                endTime,
                startDate,
                endDate,
                hall,
                user
        );

        BookingSeriesRequest saved = bookingSeriesRequestRepository.save(request);

        notificationPort.notifyAdminsAboutNewBookingSeriesRequest(request);
        return saved.getId();
    }

    public void approve(Long adminUserId, Long bookingSeriesRequestId) {

        User admin = loadActiveUser(adminUserId);

        if (!admin.isAdmin()) {
            throw new ForbiddenException("User not allowed to approve booking series requests");
        }

        BookingSeriesRequest request = bookingSeriesRequestRepository.findById(bookingSeriesRequestId)
                .orElseThrow(() -> new NotFoundException("Booking series request not found"));

        if (!request.isOpen()) {
            throw new ValidationException("Booking series request is not open");
        }

        Hall hall = request.getHall();
        if (!hall.isActive()) {
            throw new ForbiddenException("Hall inactive");
        }

        List<LocalDate> occurrences = buildOccurrences(
                request.getWeekday(),
                request.getStartDate(),
                request.getEndDate()
        );

        List<LocalDate> conflictFreeDates = occurrences.stream()
                .filter(date -> !hasConflict(
                        hall,
                        date.atTime(request.getStartTime()),
                        date.atTime(request.getEndTime())
                ))
                .toList();

        if (conflictFreeDates.isEmpty()) {
            throw new ValidationException("No conflict-free appointments available for this series request");
        }

        BookingSeries bookingSeries = BookingSeries.createNew(
                request.getTitle(),
                request.getDescription(),
                request.getWeekday(),
                request.getStartTime(),
                request.getEndTime(),
                request.getStartDate(),
                request.getEndDate(),
                hall,
                request.getRequestingUser()
        );

        BookingSeries savedSeries = bookingSeriesRepository.save(bookingSeries);

        for (LocalDate date : conflictFreeDates) {
            Booking booking = Booking.createNew(
                    request.getTitle(),
                    request.getDescription(),
                    date,
                    date.atTime(request.getStartTime()),
                    date.atTime(request.getEndTime()),
                    hall,
                    request.getRequestingUser(),
                    savedSeries
            );

            bookingRepository.save(booking);
        }

        request.approve();
        bookingSeriesRequestRepository.save(request);

        notificationPort.notifyRequesterAboutBookingSeriesRequestApproved(request, bookingSeries);

        // TODO: Optional Konflikttage für spätere Anzeige/Info separat protokollieren
    }

    public void reject(Long adminUserId, Long bookingSeriesRequestId, String reason) {

        User admin = loadActiveUser(adminUserId);

        if (!admin.isAdmin()) {
            throw new ForbiddenException("User not allowed to reject booking series requests");
        }

        BookingSeriesRequest request = bookingSeriesRequestRepository.findById(bookingSeriesRequestId)
                .orElseThrow(() -> new NotFoundException("Booking series request not found"));

        if (!request.isOpen()) {
            throw new ValidationException("Booking series request is not open");
        }

        request.reject(reason);
        bookingSeriesRequestRepository.save(request);

        notificationPort.notifyRequesterAboutBookingSeriesRequestRejected(request, reason);
    }

    public List<BookingSeriesRequest> getOpenRequests(Long adminUserId) {

        User admin = loadActiveUser(adminUserId);

        if (!admin.isAdmin()) {
            throw new ForbiddenException("User not allowed to view open booking series requests");
        }

        return bookingSeriesRequestRepository.findByStatus(BookingRequestStatus.OPEN)
                .stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }

    public List<BookingSeriesRequest> getAllRequests(Long adminUserId) {

        User admin = loadActiveUser(adminUserId);

        if (!admin.isAdmin()) {
            throw new ForbiddenException("User not allowed to view all booking series requests");
        }

        return bookingSeriesRequestRepository.findAll()
                .stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }


    public List<BookingSeriesRequest> getRequestsByUser(Long userId) {

        User user = loadActiveUser(userId);

        return bookingSeriesRequestRepository.findByRequestingUserId(user.getId())
                .stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }

    public BookingSeriesRequest getById(Long currentUserId, Long bookingSeriesRequestId) {

        User user = loadActiveUser(currentUserId);

        BookingSeriesRequest request = bookingSeriesRequestRepository.findById(bookingSeriesRequestId)
                .orElseThrow(() -> new NotFoundException("Booking series request not found"));

        if (user.isAdmin()) {
            return request;
        }

        if (request.getRequestingUser().getId().equals(user.getId())) {
            return request;
        }

        throw new ForbiddenException("User not allowed to view this booking series request");
    }

    private User loadActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!user.isActive()) {
            throw new ForbiddenException("User inactive");
        }

        return user;
    }

    private void validateCreateInput(String title,
                                     DayOfWeek weekday,
                                     LocalTime startTime,
                                     LocalTime endTime,
                                     LocalDate startDate,
                                     LocalDate endDate) {

        if (title == null || title.isBlank()) {
            throw new ValidationException("Title required");
        }

        if (weekday == null) {
            throw new ValidationException("Weekday required");
        }

        if (startTime == null || endTime == null) {
            throw new ValidationException("Start time and end time are required");
        }

        if (startDate == null || endDate == null) {
            throw new ValidationException("Start date and end date are required");
        }

        if (!startTime.isBefore(endTime)) {
            throw new ValidationException("Start time must be before end time");
        }

        if (startDate.isAfter(endDate)) {
            throw new ValidationException("Start date must be before or equal to end date");
        }

        LocalDate today = LocalDate.now(clock);
        if (startDate.isBefore(today)) {
            throw new ValidationException("Cannot create series request in the past");
        }

        LocalDate latestAllowedDate = today.plusYears(1);
        if (startDate.isAfter(latestAllowedDate) || endDate.isAfter(latestAllowedDate)) {
            throw new ValidationException("Series request may be created at most one year in advance");
        }
    }

    private void validateTimeGrid(LocalTime startTime, LocalTime endTime) {
        int interval = config.bookingIntervalMinutes();

        if (startTime.getMinute() % interval != 0 || endTime.getMinute() % interval != 0) {
            throw new ValidationException("Not on valid time grid");
        }

        if (startTime.getSecond() != 0 || endTime.getSecond() != 0
                || startTime.getNano() != 0 || endTime.getNano() != 0) {
            throw new ValidationException("Seconds and nanoseconds are not allowed");
        }
    }

    private void validateOpeningHours(LocalTime startTime, LocalTime endTime) {
        if (startTime.isBefore(config.openingStart()) || endTime.isAfter(config.openingEnd())) {
            throw new ValidationException("Outside opening hours");
        }
    }

    private List<LocalDate> buildOccurrences(DayOfWeek weekday, LocalDate startDate, LocalDate endDate) {
        List<LocalDate> dates = new ArrayList<>();

        LocalDate current = startDate;
        while (current.getDayOfWeek() != weekday) {
            current = current.plusDays(1);
            if (current.isAfter(endDate)) {
                return dates;
            }
        }

        while (!current.isAfter(endDate)) {
            dates.add(current);
            current = current.plusWeeks(1);
        }

        return dates;
    }

    private boolean hasConflict(Hall requestedHall, LocalDateTime start, LocalDateTime end) {
        return hasBookingConflict(requestedHall, start, end)
                || hasBlockedTimeConflict(requestedHall, start, end)
                || hasSeriesConflict(requestedHall, start.toLocalDate(), start.toLocalTime(), end.toLocalTime());
    }

    private boolean hasBookingConflict(Hall requestedHall, LocalDateTime start, LocalDateTime end) {
        if (requestedHall.isFullHall()) {
            return bookingRepository.findByTimeRange(start, end)
                    .stream()
                    .anyMatch(existing -> !existing.isCancelled());
        }

        boolean sameHallConflict = bookingRepository
                .findByHallIdAndTimeRange(requestedHall.getId(), start, end)
                .stream()
                .anyMatch(existing -> !existing.isCancelled());

        if (sameHallConflict) {
            return true;
        }

        return bookingRepository.findByTimeRange(start, end)
                .stream()
                .anyMatch(booking -> !booking.isCancelled() && booking.getHall().isFullHall());
    }

    private boolean hasBlockedTimeConflict(Hall requestedHall, LocalDateTime start, LocalDateTime end) {
        if (requestedHall.isFullHall()) {
            return !blockedTimeRepository.findAllByTimeRange(start, end).isEmpty();
        }

        boolean sameHallConflict = !blockedTimeRepository
                .findByHallIdAndTimeRange(requestedHall.getId(), start, end)
                .isEmpty();

        if (sameHallConflict) {
            return true;
        }

        return blockedTimeRepository.findAllByTimeRange(start, end)
                .stream()
                .anyMatch(blockedTime -> blockedTime.getHall().isFullHall());
    }

    private boolean hasSeriesConflict(Hall requestedHall,
                                      LocalDate date,
                                      LocalTime requestedStart,
                                      LocalTime requestedEnd) {

        if (requestedHall.isFullHall()) {
            return findPotentialConflictingSeriesForFullHall(date).stream()
                    .filter(series -> !series.isCancelled())
                    .anyMatch(series -> seriesOverlapsOnDate(series, date, requestedStart, requestedEnd));
        }

        boolean sameHallConflict = bookingSeriesRepository.findByHallId(requestedHall.getId())
                .stream()
                .filter(series -> !series.isCancelled())
                .anyMatch(series -> seriesOverlapsOnDate(series, date, requestedStart, requestedEnd));

        if (sameHallConflict) {
            return true;
        }

        return findPotentialFullHallSeries(date).stream()
                .filter(series -> !series.isCancelled())
                .anyMatch(series -> seriesOverlapsOnDate(series, date, requestedStart, requestedEnd));
    }
    private List<BookingSeries> findPotentialConflictingSeriesForFullHall(LocalDate date) {
        List<BookingSeries> conflictingSeries = new ArrayList<>();

        for (Hall hall : hallRepository.findAllActive()) {
            conflictingSeries.addAll(
                    bookingSeriesRepository.findActiveByHallIdAndDateRange(hall.getId(), date, date)
            );
        }

        return conflictingSeries;
    }

    private boolean seriesOverlapsOnDate(BookingSeries series,
                                         LocalDate date,
                                         LocalTime requestedStart,
                                         LocalTime requestedEnd) {

        // Serie gilt nur, wenn Datum im Bereich liegt
        if (date.isBefore(series.getStartDate()) || date.isAfter(series.getEndDate())) {
            return false;
        }

        // Serie gilt nur am richtigen Wochentag
        if (!date.getDayOfWeek().equals(series.getWeekday())) {
            return false;
        }

        // Zeitüberlappung prüfen
        return requestedStart.isBefore(series.getEndTime())
                && requestedEnd.isAfter(series.getStartTime());
    }

    private List<BookingSeries> findPotentialFullHallSeries(LocalDate date) {
        return hallRepository.findAllActive().stream()
                .filter(Hall::isFullHall)
                .flatMap(hall ->
                        bookingSeriesRepository
                                .findActiveByHallIdAndDateRange(hall.getId(), date, date)
                                .stream()
                )
                .toList();
    }
}