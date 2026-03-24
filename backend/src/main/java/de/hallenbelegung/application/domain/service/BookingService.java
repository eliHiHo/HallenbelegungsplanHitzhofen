package de.hallenbelegung.application.domain.service;

import de.hallenbelegung.application.domain.exception.ForbiddenException;
import de.hallenbelegung.application.domain.exception.NotFoundException;
import de.hallenbelegung.application.domain.exception.ValidationException;
import de.hallenbelegung.application.domain.model.Booking;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.in.CancelBookingUseCase;
import de.hallenbelegung.application.domain.port.in.GetBookingUseCase;
import de.hallenbelegung.application.domain.port.in.GetUserBookingsUseCase;
import de.hallenbelegung.application.domain.port.in.UpdateBookingFeedbackUseCase;
import de.hallenbelegung.application.domain.port.out.BookingRepositoryPort;
import de.hallenbelegung.application.domain.port.out.UserRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
@Transactional
public class BookingService implements
        GetBookingUseCase,
        GetUserBookingsUseCase,
        CancelBookingUseCase,
        UpdateBookingFeedbackUseCase    {


    private final BookingRepositoryPort bookingRepository;
    private final UserRepositoryPort userRepository;

    public BookingService(
            BookingRepositoryPort bookingRepository,
            UserRepositoryPort userRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    public Booking getById(Long currentUserId, Long bookingId) {
        User user = loadActiveUser(currentUserId);
        Booking booking = loadBooking(bookingId);

        if (user.isAdmin() || isResponsibleUser(user, booking)) {
            return booking;
        }

        throw new ForbiddenException("User not allowed to view this booking");
    }

    public List<Booking> getBookingsByUser(Long userId) {
        User user = loadActiveUser(userId);

        return bookingRepository.findByResponsibleUserId(user.getId())
                .stream()
                .sorted((a, b) -> b.getStartDateTime().compareTo(a.getStartDateTime()))
                .toList();
    }

    public void cancel(Long currentUserId, Long bookingId, String cancellationReason) {
        User user = loadActiveUser(currentUserId);
        Booking booking = loadBooking(bookingId);

        if (booking.isCancelled()) {
            throw new ValidationException("Booking is already cancelled");
        }

        if (!user.isAdmin() && !isResponsibleUser(user, booking)) {
            throw new ForbiddenException("User not allowed to cancel this booking");
        }

        booking.cancel(cancellationReason);
        bookingRepository.save(booking);

        // später: NotificationPort
    }

    public void addFeedback(Long currentUserId,
                            Long bookingId,
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
    public void cancel(Long bookingId, Long userId) {
        cancel(userId, bookingId, null);
    }

    @Override
    public void updateFeedback(Long bookingId, Integer participantCount, String comment, Long userId) {
        addFeedback(userId, bookingId, participantCount, comment);
    }

    private User loadActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!user.isActive()) {
            throw new ForbiddenException("User inactive");
        }

        return user;
    }

    private Booking loadBooking(Long bookingId) {
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
}