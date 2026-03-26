package de.hallenbelegung.application.domain.service;

import de.hallenbelegung.application.domain.exception.ForbiddenException;
import de.hallenbelegung.application.domain.exception.NotFoundException;
import de.hallenbelegung.application.domain.exception.ValidationException;
import de.hallenbelegung.application.domain.model.Booking;
import de.hallenbelegung.application.domain.model.Hall;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.in.CancelBookingUseCase;
import de.hallenbelegung.application.domain.port.in.GetBookingUseCase;
import de.hallenbelegung.application.domain.port.in.GetUserBookingsUseCase;
import de.hallenbelegung.application.domain.port.in.UpdateBookingFeedbackUseCase;
import de.hallenbelegung.application.domain.port.in.UpdateBookingUseCase;
import de.hallenbelegung.application.domain.port.out.BlockedTimeRepositoryPort;
import de.hallenbelegung.application.domain.port.out.BookingRepositoryPort;
import de.hallenbelegung.application.domain.port.out.HallRepositoryPort;
import de.hallenbelegung.application.domain.port.out.NotificationPort;
import de.hallenbelegung.application.domain.port.out.UserRepositoryPort;
import de.hallenbelegung.application.domain.view.BookingDetailView;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import de.hallenbelegung.application.domain.port.out.HallConfigPort;
@ApplicationScoped
@Transactional
public class BookingService implements
        GetBookingUseCase,
        GetUserBookingsUseCase,
        CancelBookingUseCase,
        UpdateBookingFeedbackUseCase,
        UpdateBookingUseCase {

    private final NotificationPort notificationPort;
    private final BookingRepositoryPort bookingRepository;
    private final UserRepositoryPort userRepository;
    private final HallRepositoryPort hallRepository;
    private final BlockedTimeRepositoryPort blockedTimeRepository;
    private final HallConfigPort config;
    private final Clock clock;

    public BookingService(
            BookingRepositoryPort bookingRepository,
            UserRepositoryPort userRepository,
            NotificationPort notificationPort,
            HallRepositoryPort hallRepository,
            BlockedTimeRepositoryPort blockedTimeRepository,
            HallConfigPort config,
            Clock clock
    ) {
        this.notificationPort = notificationPort;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.hallRepository = hallRepository;
        this.blockedTimeRepository = blockedTimeRepository;
        this.config = config;
        this.clock = clock;
    }

    @Override
    public BookingDetailView getById(UUID currentUserId, UUID bookingId) {
        Booking booking = loadBooking(bookingId);

        if (currentUserId == null) {
            return toBookingDetailView(booking, false, false, false);
        }

        User user = loadActiveUser(currentUserId);

        if (user.isAdmin()) {
            return toBookingDetailView(booking, true, true, true);
        }

        if (isResponsibleUser(user, booking)) {
            return toBookingDetailView(booking, true, false, true);
        }

        return toBookingDetailView(booking, false, false, false);
    }

    @Override
    public List<Booking> getBookingsByUser(UUID userId) {
        User user = loadActiveUser(userId);

        return bookingRepository.findByResponsibleUserId(user.getId())
                .stream()
                .sorted((a, b) -> b.getstartAt().compareTo(a.getstartAt()))
                .toList();
    }

    @Override
    public void cancel(UUID currentUserId, UUID bookingId, String cancellationReason) {
        User user = loadActiveUser(currentUserId);
        Booking booking = loadBooking(bookingId);

        if (booking.isCancelled()) {
            throw new ValidationException("Booking is already cancelled");
        }

        if (!user.isAdmin() && !isResponsibleUser(user, booking)) {
            throw new ForbiddenException("User not allowed to cancel this booking");
        }

        boolean cancelledByAdmin = user.isAdmin();

        booking.cancel(user, cancellationReason);
        bookingRepository.save(booking);

        if (cancelledByAdmin) {
            notificationPort.notifyRequesterAboutBookingCancelledByAdmin(
                    booking,
                    cancellationReason
            );
        }
    }

    public void addFeedback(UUID currentUserId,
                            UUID bookingId,
                            Integer participantCount,
                            String feedbackComment) {
        User user = loadActiveUser(currentUserId);
        Booking booking = loadBooking(bookingId);

        if (booking.isCancelled()) {
            throw new ValidationException("Cannot add feedback to cancelled booking");
        }

        if (!user.isAdmin() && !isResponsibleUser(user, booking)) {
            throw new ForbiddenException("User not allowed to add feedback to this booking");
        }

        validateParticipantCount(participantCount);

        booking.addFeedback(participantCount, feedbackComment);
        bookingRepository.save(booking);
    }

    @Override
    public void updateFeedback(UUID bookingId, Integer participantCount, String comment, UUID userId) {
        addFeedback(userId, bookingId, participantCount, comment);
    }

    @Override
    public Booking update(UUID currentUserId,
                          UUID bookingId,
                          UUID hallId,
                          String title,
                          String description,
                          LocalDateTime startTime,
                          LocalDateTime endTime) {

        User user = loadActiveUser(currentUserId);

        if (!user.isAdmin()) {
            throw new ForbiddenException("User not allowed to update bookings");
        }

        Booking booking = loadBooking(bookingId);

        if (booking.isCancelled()) {
            throw new ValidationException("Cancelled booking cannot be updated");
        }

        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new NotFoundException("Hall not found"));

        if (!hall.isActive()) {
            throw new ForbiddenException("Hall inactive");
        }

        validateUpdateInput(title, startTime, endTime);
        validateTimeGrid(startTime, endTime);
        validateOpeningHours(startTime.toLocalTime(), endTime.toLocalTime());
        checkForConflictsExcludingCurrentBooking(booking.getId(), hall, startTime, endTime);

        booking.updateDetails(
                title,
                description,
                startTime,
                endTime,
                hall
        );

        Booking savedBooking = bookingRepository.save(booking);
        notificationPort.notifyRequesterAboutBookingUpdated(savedBooking);

        return savedBooking;
    }

    private User loadActiveUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!user.isActive()) {
            throw new ForbiddenException("User inactive");
        }

        return user;
    }

    private Booking loadBooking(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));
    }

    private boolean isResponsibleUser(User user, Booking booking) {
        return booking.getResponsibleUser().getId().equals(user.getId());
    }

    private void validateParticipantCount(Integer participantCount) {
        if (participantCount != null && participantCount < 0) {
            throw new ValidationException("Participant count must not be negative");
        }
    }

    private void validateUpdateInput(String title, LocalDateTime startTime, LocalDateTime endTime) {
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
            throw new ValidationException("Cannot move booking into the past");
        }

        if (startTime.isAfter(now.plusYears(1))) {
            throw new ValidationException("Booking must not be more than one year in advance");
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

    private void checkForConflictsExcludingCurrentBooking(UUID currentBookingId,
                                                          Hall requestedHall,
                                                          LocalDateTime start,
                                                          LocalDateTime end) {

        if (requestedHall.isFullHall()) {
            boolean bookingConflict = bookingRepository.findByTimeRange(start, end).stream()
                    .anyMatch(existing ->
                            !existing.getId().equals(currentBookingId) && !existing.isCancelled()
                    );

            if (bookingConflict) {
                throw new ValidationException("Conflict with existing booking");
            }

            boolean blockedConflict = !blockedTimeRepository.findAllByTimeRange(start, end).isEmpty();

            if (blockedConflict) {
                throw new ValidationException("Conflict with blocked time");
            }

            return;
        }

        boolean sameHallBookingConflict = bookingRepository.findByHallIdAndTimeRange(requestedHall.getId(), start, end)
                .stream()
                .anyMatch(existing ->
                        !existing.getId().equals(currentBookingId) && !existing.isCancelled()
                );

        if (sameHallBookingConflict) {
            throw new ValidationException("Conflict with existing booking");
        }

        boolean fullHallBookingConflict = bookingRepository.findByTimeRange(start, end)
                .stream()
                .anyMatch(existing ->
                        !existing.getId().equals(currentBookingId)
                                && !existing.isCancelled()
                                && existing.getHall().isFullHall()
                );

        if (fullHallBookingConflict) {
            throw new ValidationException("Conflict with full hall booking");
        }

        boolean sameHallBlockedConflict = !blockedTimeRepository
                .findByHallIdAndTimeRange(requestedHall.getId(), start, end)
                .isEmpty();

        if (sameHallBlockedConflict) {
            throw new ValidationException("Conflict with blocked time");
        }

        boolean fullHallBlockedConflict = blockedTimeRepository.findAllByTimeRange(start, end)
                .stream()
                .anyMatch(blockedTime -> blockedTime.getHall().isFullHall());

        if (fullHallBlockedConflict) {
            throw new ValidationException("Conflict with full hall blocked time");
        }
    }

    private BookingDetailView toBookingDetailView(Booking booking,
                                                  boolean canViewFeedback,
                                                  boolean canEdit,
                                                  boolean canCancel) {
        return new BookingDetailView(
                booking.getId(),
                booking.getTitle(),
                booking.getDescription(),
                booking.getstartAt(),
                booking.getendAt(),
                booking.getHall().getId(),
                booking.getHall().getName(),
                booking.getStatus().name(),
                booking.getResponsibleUser().getFullName(),
                canViewFeedback ? booking.getParticipantCount() : null,
                canViewFeedback ? booking.getFeedbackComment() : null,
                canViewFeedback,
                canEdit,
                canCancel
        );
    }
}