package de.hallenbelegung.application.domain.service;

import de.hallenbelegung.application.domain.exception.BookingConflictException;
import de.hallenbelegung.application.domain.exception.ForbiddenException;
import de.hallenbelegung.application.domain.exception.NotFoundException;
import de.hallenbelegung.application.domain.exception.ValidationException;
import de.hallenbelegung.application.domain.model.Booking;
import de.hallenbelegung.application.domain.model.BookingRequest;
import de.hallenbelegung.application.domain.model.BookingRequestStatus;
import de.hallenbelegung.application.domain.model.Hall;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.in.ApproveBookingRequestUseCase;
import de.hallenbelegung.application.domain.port.in.CreateBookingRequestUseCase;
import de.hallenbelegung.application.domain.port.in.RejectBookingRequestUseCase;
import de.hallenbelegung.application.domain.port.out.BlockedTimeRepositoryPort;
import de.hallenbelegung.application.domain.port.out.BookingRepositoryPort;
import de.hallenbelegung.application.domain.port.out.BookingRequestRepositoryPort;
import de.hallenbelegung.application.domain.port.out.HallRepositoryPort;
import de.hallenbelegung.application.domain.port.out.UserRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@ApplicationScoped
@Transactional
public class BookingRequestService implements
        CreateBookingRequestUseCase,
        ApproveBookingRequestUseCase,
        RejectBookingRequestUseCase {

    private final BookingRequestRepositoryPort bookingRequestRepository;
    private final BookingRepositoryPort bookingRepository;
    private final BlockedTimeRepositoryPort blockedTimeRepository;
    private final UserRepositoryPort userRepository;
    private final HallRepositoryPort hallRepository;
    private final HallenbelegungConfig config;
    private final Clock clock;

    public BookingRequestService(
            BookingRequestRepositoryPort bookingRequestRepository,
            BookingRepositoryPort bookingRepository,
            BlockedTimeRepositoryPort blockedTimeRepository,
            UserRepositoryPort userRepository,
            HallRepositoryPort hallRepository,
            HallenbelegungConfig config,
            Clock clock
    ) {
        this.bookingRequestRepository = bookingRequestRepository;
        this.bookingRepository = bookingRepository;
        this.blockedTimeRepository = blockedTimeRepository;
        this.userRepository = userRepository;
        this.hallRepository = hallRepository;
        this.config = config;
        this.clock = clock;
    }

    @Override
    public Long create(Long userId,
                       Long hallId,
                       String title,
                       String description,
                       LocalDateTime startTime,
                       LocalDateTime endTime) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!user.isClubRepresentative() && !user.isAdmin()) {
            throw new ForbiddenException("User not allowed to create booking request");
        }

        if (!user.isActive()) {
            throw new ForbiddenException("User inactive");
        }

        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new NotFoundException("Hall not found"));

        if (!hall.isActive()) {
            throw new ForbiddenException("Hall inactive");
        }

        validateCreateInput(title, startTime, endTime);
        validateTimeGrid(startTime, endTime);
        validateOpeningHours(startTime.toLocalTime(), endTime.toLocalTime());

        checkForConflicts(hall, startTime, endTime);

        BookingRequest request = BookingRequest.createNew(
                title,
                description,
                startTime.toLocalDate(),
                startTime,
                endTime,
                hall,
                user
        );

        BookingRequest saved = bookingRequestRepository.save(request);

        // TODO: NotificationPort verwenden, um Admin über neue BookingRequest zu informieren

        return saved.getId();
    }

    public void approve(Long adminUserId, Long bookingRequestId) {

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new NotFoundException("Admin user not found"));

        if (!admin.isAdmin()) {
            throw new ForbiddenException("User not allowed to approve booking requests");
        }

        if (!admin.isActive()) {
            throw new ForbiddenException("Admin user inactive");
        }

        BookingRequest request = bookingRequestRepository.findById(bookingRequestId)
                .orElseThrow(() -> new NotFoundException("Booking request not found"));

        if (!request.isOpen()) {
            throw new ValidationException("Booking request is not open");
        }

        Hall hall = request.getHall();

        if (!hall.isActive()) {
            throw new ForbiddenException("Hall inactive");
        }

        LocalDateTime startTime = request.getStartDateTime();
        LocalDateTime endTime = request.getEndDateTime();

        checkForConflicts(hall, startTime, endTime);

        Booking booking = Booking.createNew(
                request.getTitle(),
                request.getDescription(),
                request.getDate(),
                request.getStartDateTime(),
                request.getEndDateTime(),
                request.getHall(),
                request.getRequestingUser(),
                null
        );

        Booking savedBooking = bookingRepository.save(booking);

        request.approve();
        bookingRequestRepository.save(request);

        // TODO: NotificationPort verwenden, um Antragsteller über Genehmigung zu informieren
    }

    public void reject(Long adminUserId, Long bookingRequestId, String reason) {

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new NotFoundException("Admin user not found"));

        if (!admin.isAdmin()) {
            throw new ForbiddenException("User not allowed to reject booking requests");
        }

        if (!admin.isActive()) {
            throw new ForbiddenException("Admin user inactive");
        }

        BookingRequest request = bookingRequestRepository.findById(bookingRequestId)
                .orElseThrow(() -> new NotFoundException("Booking request not found"));

        if (!request.isOpen()) {
            throw new ValidationException("Booking request is not open");
        }

        request.reject(reason);
        bookingRequestRepository.save(request);

        // TODO: NotificationPort verwenden, um Antragsteller über Ablehnung zu informieren
    }

    public List<BookingRequest> getOpenRequests(Long adminUserId) {

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new NotFoundException("Admin user not found"));

        if (!admin.isAdmin()) {
            throw new ForbiddenException("User not allowed to view open booking requests");
        }

        if (!admin.isActive()) {
            throw new ForbiddenException("Admin user inactive");
        }

        return bookingRequestRepository.findByStatus(BookingRequestStatus.OPEN)
                .stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }

    public List<BookingRequest> getRequestsByUser(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!user.isActive()) {
            throw new ForbiddenException("User inactive");
        }

        return bookingRequestRepository.findByRequestingUserId(userId)
                .stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }

    public BookingRequest getById(Long currentUserId, Long bookingRequestId) {

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!user.isActive()) {
            throw new ForbiddenException("User inactive");
        }

        BookingRequest request = bookingRequestRepository.findById(bookingRequestId)
                .orElseThrow(() -> new NotFoundException("Booking request not found"));

        if (user.isAdmin()) {
            return request;
        }

        if (request.getRequestingUser().getId().equals(user.getId())) {
            return request;
        }

        throw new ForbiddenException("User not allowed to view this booking request");
    }

    private void validateCreateInput(String title, LocalDateTime startTime, LocalDateTime endTime) {
        if (title == null || title.isBlank()) {
            throw new ValidationException("Title required");
        }

        if (startTime == null || endTime == null) {
            throw new ValidationException("Start time and end time are required");
        }

        if (!startTime.isBefore(endTime)) {
            throw new ValidationException("Start must be before end");
        }

        if (startTime.isBefore(LocalDateTime.now(clock))) {
            throw new ValidationException("Cannot book in the past");
        }
    }

    private void validateTimeGrid(LocalDateTime startTime, LocalDateTime endTime) {
        int interval = config.bookingIntervalMinutes();

        if (startTime.getMinute() % interval != 0 || endTime.getMinute() % interval != 0) {
            throw new ValidationException("Not on valid time grid");
        }

        if (startTime.getSecond() != 0 || endTime.getSecond() != 0
                || startTime.getNano() != 0 || endTime.getNano() != 0) {
            throw new ValidationException("Seconds and nanoseconds are not allowed");
        }
    }

    private void validateOpeningHours(LocalTime start, LocalTime end) {
        if (start.isBefore(config.openingStart()) || end.isAfter(config.openingEnd())) {
            throw new ValidationException("Outside opening hours");
        }
    }

    private void checkForConflicts(Hall requestedHall,
                                   LocalDateTime start,
                                   LocalDateTime end) {

        if (requestedHall.isFullHall()) {
            boolean bookingConflict = !bookingRepository.findByTimeRange(start, end).isEmpty();
            if (bookingConflict) {
                throw new BookingConflictException("Conflict with existing booking");
            }

            boolean blockedConflict = !blockedTimeRepository.findAllByTimeRange(start, end).isEmpty();
            if (blockedConflict) {
                throw new BookingConflictException("Conflict with blocked time");
            }

            return;
        }

        boolean sameHallBookingConflict = !bookingRepository
                .findByHallIdAndTimeRange(requestedHall.getId(), start, end)
                .isEmpty();

        if (sameHallBookingConflict) {
            throw new BookingConflictException("Conflict with existing booking");
        }

        boolean fullHallBookingConflict = bookingRepository
                .findByTimeRange(start, end)
                .stream()
                .anyMatch(booking -> booking.getHall().isFullHall());

        if (fullHallBookingConflict) {
            throw new BookingConflictException("Conflict with full hall booking");
        }

        boolean sameHallBlockedConflict = !blockedTimeRepository
                .findByHallIdAndTimeRange(requestedHall.getId(), start, end)
                .isEmpty();

        if (sameHallBlockedConflict) {
            throw new BookingConflictException("Conflict with blocked time");
        }

        boolean fullHallBlockedConflict = blockedTimeRepository
                .findAllByTimeRange(start, end)
                .stream()
                .anyMatch(blockedTime -> blockedTime.getHall().isFullHall());

        if (fullHallBlockedConflict) {
            throw new BookingConflictException("Conflict with full hall blocked time");
        }
    }
}