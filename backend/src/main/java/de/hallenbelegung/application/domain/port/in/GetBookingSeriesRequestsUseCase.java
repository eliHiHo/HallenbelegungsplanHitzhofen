package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.model.BookingSeriesRequest;

import java.util.List;

public interface GetBookingSeriesRequestsUseCase {
    List<BookingSeriesRequest> getOpenRequests(Long adminUserId);
    List<BookingSeriesRequest> getRequestsByUser(Long userId);
}