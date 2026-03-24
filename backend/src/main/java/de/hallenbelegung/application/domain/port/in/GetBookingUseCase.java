package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.model.Booking;

public interface GetBookingUseCase {
    Booking getById(Long currentUserId, Long bookingId);
}
