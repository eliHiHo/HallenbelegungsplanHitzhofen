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
import de.hallenbelegung.application.domain.port.in.*;
import de.hallenbelegung.application.domain.port.out.BlockedTimeRepositoryPort;
import de.hallenbelegung.application.domain.port.out.BookingRepositoryPort;
import de.hallenbelegung.application.domain.port.out.BookingRequestRepositoryPort;
import de.hallenbelegung.application.domain.port.out.HallRepositoryPort;
import de.hallenbelegung.application.domain.port.out.UserRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import de.hallenbelegung.application.domain.port.out.NotificationPort;
import de.hallenbelegung.application.domain.port.out.HallConfigPort;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
@ApplicationScoped
@Transactional
public class BookingRequestService implements
        ApproveBookingRequestUseCase,
        RejectBookingRequestUseCase,
        CreateBookingRequestUseCase,
        GetBookingRequestUseCase,
        GetBookingRequestsUseCase  {

    private final BookingRequestRepositoryPort bookingRequestRepository;
    private final BookingRepositoryPort bookingRepository;
    private final BlockedTimeRepositoryPort blockedTimeRepository;
    private final UserRepositoryPort userRepository;
    private final HallRepositoryPort hallRepository;
    private final HallConfigPort config;
    private final Clock clock;
    private final NotificationPort notificationPort;

    public BookingRequestService(
            BookingRequestRepositoryPort bookingRequestRepository,
            BookingRepositoryPort bookingRepository,
            BlockedTimeRepositoryPort blockedTimeRepository,
            UserRepositoryPort userRepository,
            HallRepositoryPort hallRepository,
            HallConfigPort config,
            Clock clock,
            NotificationPort notificationPort
    ) {
        this.bookingRequestRepository = bookingRequestRepository;
        this.bookingRepository = bookingRepository;
        this.blockedTimeRepository = blockedTimeRepository;
        this.userRepository = userRepository;
        this.hallRepository = hallRepository;
        this.config = config;
        this.clock = clock;
        this.notificationPort = notificationPort;
    }

    @Override
    public UUID create(UUID userId,
                       UUID hallId,
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
                startTime,
                endTime,
                hall,
                user
        );

        BookingRequest saved = bookingRequestRepository.save(request);

        try {
            notificationPort.notifyAdminsAboutNewBookingRequest(request);
        } catch (Exception ignored) {
            // Notification failure must not roll back the request creation
        }
        return saved.getId();
    }

    public void approve(UUID adminUserId, UUID bookingRequestId) {

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

        if (!request.isPending()) {
            throw new ValidationException("Booking request is not open");
        }

        Hall hall = request.getHall();

        if (!hall.isActive()) {
            throw new ForbiddenException("Hall inactive");
        }

        LocalDateTime startTime = request.getStartAt();
        LocalDateTime endTime = request.getEndAt();

        checkForConflicts(hall, startTime, endTime);

        Booking booking = Booking.createNew(
                request.getTitle(),
                request.getDescription(),
                request.getStartAt(),
                request.getEndAt(),
                request.getHall(),
                request.getRequestedBy(),
                null,
                admin, // createdBy: the admin who approves/creates the booking
                null,
                null,
                null,
                null
        );

        Booking savedBooking = bookingRepository.save(booking);

        request.approve(admin);
        bookingRequestRepository.save(request);

        try {
            notificationPort.notifyRequesterAboutBookingRequestApproved(request, savedBooking);
        } catch (Exception ignored) {
            // Notification failure must not roll back the approval
        }
    }

    public void reject(UUID adminUserId, UUID bookingRequestId, String reason) {

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

        if (!request.isPending()) {
            throw new ValidationException("Booking request is not open");
        }

        request.reject(admin, reason);
        bookingRequestRepository.save(request);

        try {
            notificationPort.notifyRequesterAboutBookingRequestRejected(request, reason);
        } catch (Exception ignored) {
            // Notification failure must not roll back the rejection
        }
    }

    public List<BookingRequest> getOpenRequests(UUID adminUserId) {

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new NotFoundException("Admin user not found"));

        if (!admin.isAdmin()) {
            throw new ForbiddenException("User not allowed to view open booking requests");
        }

        if (!admin.isActive()) {
            throw new ForbiddenException("Admin user inactive");
        }

        return bookingRequestRepository.findByStatus(BookingRequestStatus.PENDING)
                .stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }

    public List<BookingRequest> getAllRequests(UUID adminUserId) {

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new NotFoundException("Admin user not found"));

        if (!admin.isAdmin()) {
            throw new ForbiddenException("User not allowed to view all booking requests");
        }

        if (!admin.isActive()) {
            throw new ForbiddenException("Admin user inactive");
        }

        return bookingRequestRepository.findAll()
                .stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }

    public List<BookingRequest> getRequestsByUser(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!user.isActive()) {
            throw new ForbiddenException("User inactive");
        }

        return bookingRequestRepository.findByRequestedByUserId(userId)
                .stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }

    public BookingRequest getById(UUID currentUserId, UUID bookingRequestId) {

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

        if (request.getRequestedBy().getId().equals(user.getId())) {
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

        LocalDateTime now = LocalDateTime.now(clock);

        if (startTime.isBefore(now)) {
            throw new ValidationException("Cannot book in the past");
        }

        if (startTime.isAfter(now.plusYears(1))) {
            throw new ValidationException("Booking request must not be more than one year in advance");
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
            boolean bookingConflict = bookingRepository.findByTimeRange(start, end)
                    .stream()
                    .anyMatch(existing -> !existing.isCancelled());
            if (bookingConflict) {
                throw new BookingConflictException("Conflict with existing booking");
            }

            boolean blockedConflict = !blockedTimeRepository.findAllByTimeRange(start, end).isEmpty();
            if (blockedConflict) {
                throw new BookingConflictException("Conflict with blocked time");
            }

            return;
        }

        boolean sameHallBookingConflict = bookingRepository
                .findByHallIdAndTimeRange(requestedHall.getId(), start, end)
                .stream()
                .anyMatch(existing -> !existing.isCancelled());

        if (sameHallBookingConflict) {
            throw new BookingConflictException("Conflict with existing booking");
        }

        boolean fullHallBookingConflict = bookingRepository
                .findByTimeRange(start, end)
                .stream()
                .anyMatch(booking -> !booking.isCancelled() && booking.getHall().isFullHall());

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