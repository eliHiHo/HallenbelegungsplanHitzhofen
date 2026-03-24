package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.model.BookingSeriesRequest;

public interface GetBookingSeriesRequestUseCase {
    BookingSeriesRequest getById(Long currentUserId, Long bookingSeriesRequestId);
}