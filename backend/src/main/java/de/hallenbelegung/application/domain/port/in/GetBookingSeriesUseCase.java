package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.model.BookingSeries;

import java.util.UUID;

public interface GetBookingSeriesUseCase {
    BookingSeries getById(UUID currentUserId, UUID bookingSeriesId);
}
