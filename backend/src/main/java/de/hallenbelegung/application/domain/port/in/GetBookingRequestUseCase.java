package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.model.BookingRequest;

import java.util.UUID;

public interface GetBookingRequestUseCase {
    BookingRequest getById(UUID currentUserId, UUID bookingRequestId);
}