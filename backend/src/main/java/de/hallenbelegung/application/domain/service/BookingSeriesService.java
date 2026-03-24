package de.hallenbelegung.application.domain.service;

import de.hallenbelegung.application.domain.exception.ForbiddenException;
import de.hallenbelegung.application.domain.exception.NotFoundException;
import de.hallenbelegung.application.domain.exception.ValidationException;
import de.hallenbelegung.application.domain.model.Booking;
import de.hallenbelegung.application.domain.model.BookingSeries;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.in.CancelBookingSeriesOccurrenceUseCase;
import de.hallenbelegung.application.domain.port.in.CancelBookingSeriesUseCase;
import de.hallenbelegung.application.domain.port.in.GetBookingSeriesUseCase;
import de.hallenbelegung.application.domain.port.out.BookingRepositoryPort;
import de.hallenbelegung.application.domain.port.out.BookingSeriesRepositoryPort;
import de.hallenbelegung.application.domain.port.out.UserRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
@Transactional
public class BookingSeriesService implements
        GetBookingSeriesUseCase,
        CancelBookingSeriesOccurrenceUseCase,
        CancelBookingSeriesUseCase
{

    private final BookingSeriesRepositoryPort bookingSeriesRepository;
    private final BookingRepositoryPort bookingRepository;
    private final UserRepositoryPort userRepository;

    public BookingSeriesService(
            BookingSeriesRepositoryPort bookingSeriesRepository,
            BookingRepositoryPort bookingRepository,
            UserRepositoryPort userRepository
    ) {
        this.bookingSeriesRepository = bookingSeriesRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    public BookingSeries getById(Long currentUserId, Long bookingSeriesId) {
        User user = loadActiveUser(currentUserId);
        BookingSeries bookingSeries = loadSeries(bookingSeriesId);

        if (user.isAdmin() || isResponsibleUser(user, bookingSeries)) {
            return bookingSeries;
        }

        throw new ForbiddenException("User not allowed to view this booking series");
    }

    public List<BookingSeries> getSeriesByUser(Long userId) {
        User user = loadActiveUser(userId);

        return bookingSeriesRepository.findByResponsibleUserId(user.getId())
                .stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }

    public void cancelSeries(Long currentUserId, Long bookingSeriesId, String cancellationReason) {
        User user = loadActiveUser(currentUserId);
        BookingSeries bookingSeries = loadSeries(bookingSeriesId);

        if (!user.isAdmin() && !isResponsibleUser(user, bookingSeries)) {
            throw new ForbiddenException("User not allowed to cancel this booking series");
        }

        if (bookingSeries.isCancelled()) {
            throw new ValidationException("Booking series is already cancelled");
        }

        bookingSeries.cancel(cancellationReason);
        bookingSeriesRepository.save(bookingSeries);

        List<Booking> bookings = bookingRepository.findByBookingSeriesId(bookingSeries.getId());

        for (Booking booking : bookings) {
            if (!booking.isCancelled()) {
                booking.cancel(cancellationReason);
                bookingRepository.save(booking);
            }
        }

        // TODO: NotificationPort für Info an betroffene Nutzer/Admin
    }

    public void cancelSingleOccurrence(Long currentUserId,
                                       Long bookingSeriesId,
                                       Long bookingId,
                                       String cancellationReason) {

        User user = loadActiveUser(currentUserId);
        BookingSeries bookingSeries = loadSeries(bookingSeriesId);
        Booking booking = loadBooking(bookingId);

        if (!user.isAdmin() && !isResponsibleUser(user, bookingSeries)) {
            throw new ForbiddenException("User not allowed to cancel occurrences of this booking series");
        }

        if (bookingSeries.isCancelled()) {
            throw new ValidationException("Cannot cancel a single occurrence of an already cancelled booking series");
        }

        if (booking.getBookingSeries() == null) {
            throw new ValidationException("Booking does not belong to a booking series");
        }

        if (!booking.getBookingSeries().getId().equals(bookingSeries.getId())) {
            throw new ValidationException("Booking does not belong to the specified booking series");
        }

        if (booking.isCancelled()) {
            throw new ValidationException("Booking occurrence is already cancelled");
        }

        booking.cancel(cancellationReason);
        bookingRepository.save(booking);
    }

    private User loadActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!user.isActive()) {
            throw new ForbiddenException("User inactive");
        }

        return user;
    }

    private BookingSeries loadSeries(Long bookingSeriesId) {
        return bookingSeriesRepository.findById(bookingSeriesId)
                .orElseThrow(() -> new NotFoundException("Booking series not found"));
    }

    private Booking loadBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));
    }

    private boolean isResponsibleUser(User user, BookingSeries bookingSeries) {
        return bookingSeries.getResponsibleUser().getId().equals(user.getId());
    }
}