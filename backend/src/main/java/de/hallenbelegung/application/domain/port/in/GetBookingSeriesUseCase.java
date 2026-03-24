package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.model.BookingSeries;

public interface GetBookingSeriesUseCase {
    BookingSeries getById(Long currentUserId, Long bookingSeriesId);
}
