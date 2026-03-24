package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.model.BookingSeriesRequest;

import java.util.UUID;

public interface GetBookingSeriesRequestUseCase {
    BookingSeriesRequest getById(UUID currentUserId, UUID bookingSeriesRequestId);
}