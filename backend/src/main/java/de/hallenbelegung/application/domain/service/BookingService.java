package de.hallenbelegung.application.domain.service;

import de.hallenbelegung.application.domain.exception.ForbiddenException;
import de.hallenbelegung.application.domain.exception.NotFoundException;
import de.hallenbelegung.application.domain.exception.ValidationException;
import de.hallenbelegung.application.domain.model.Booking;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.in.CancelBookingUseCase;
import de.hallenbelegung.application.domain.port.in.UpdateBookingFeedbackUseCase;
import de.hallenbelegung.application.domain.port.out.BookingRepositoryPort;
import de.hallenbelegung.application.domain.port.out.UserRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
@Transactional
public class BookingService implements
        CancelBookingUseCase,
        UpdateBookingFeedbackUseCase {

    private final BookingRepositoryPort bookingRepository;
    private final UserRepositoryPort userRepository;

    public BookingService(
            BookingRepositoryPort bookingRepository,
            UserRepositoryPort userRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    public void cancel(Long currentUserId, Long bookingId, String cancellationReason) {

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!user.isActive()) {
            throw new ForbiddenException("User inactive");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        if (booking.isCancelled()) {
            throw new ValidationException("Booking is already cancelled");
        }

        boolean isAdmin = user.isAdmin();
        boolean isResponsibleUser = booking.getResponsibleUser().getId().equals(user.getId());

        if (!isAdmin && !isResponsibleUser) {
            throw new ForbiddenException("User not allowed to cancel this booking");
        }

        booking.cancel(cancellationReason);
        bookingRepository.save(booking);

        // TODO: NotificationPort verwenden, um betroffene Nutzer über Stornierung zu informieren
    }

    public Booking getById(Long currentUserId, Long bookingId) {

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!user.isActive()) {
            throw new ForbiddenException("User inactive");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        if (user.isAdmin()) {
            return booking;
        }

        boolean isResponsibleUser = booking.getResponsibleUser()
                .getId()
                .equals(user.getId());

        if (isResponsibleUser) {
            return booking;
        }

        throw new ForbiddenException("User not allowed to view this booking");
    }

    public List<Booking> getBookingsByUser(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!user.isActive()) {
            throw new ForbiddenException("User inactive");
        }

        return bookingRepository
                .findByResponsibleUserId(userId)
                .stream()
                .sorted((a, b) -> b.getStartDateTime().compareTo(a.getStartDateTime()))
                .toList();
    }

    public void addFeedback(Long currentUserId,
                            Long bookingId,
                            Integer participantCount,
                            String feedbackComment) {

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!user.isActive()) {
            throw new ForbiddenException("User inactive");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        if (booking.isCancelled()) {
            throw new ValidationException("Cannot add feedback to cancelled booking");
        }

        boolean isAdmin = user.isAdmin();
        boolean isResponsibleUser = booking.getResponsibleUser()
                .getId()
                .equals(user.getId());

        if (!isAdmin && !isResponsibleUser) {
            throw new ForbiddenException("User not allowed to add feedback to this booking");
        }

        if (participantCount != null && participantCount < 0) {
            throw new ValidationException("Participant count must not be negative");
        }

        booking.addFeedback(participantCount, feedbackComment);
        bookingRepository.save(booking);
    }

    @Override
    public void cancel(Long bookingId, Long userId) {

    }

    @Override
    public void updateFeedback(Long bookingId, Integer participantCount, String comment, Long userId) {

    }
}