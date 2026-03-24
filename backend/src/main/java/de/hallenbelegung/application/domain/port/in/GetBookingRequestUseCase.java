package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.model.BookingRequest;

public interface GetBookingRequestUseCase {
    BookingRequest getById(Long currentUserId, Long bookingRequestId);
}