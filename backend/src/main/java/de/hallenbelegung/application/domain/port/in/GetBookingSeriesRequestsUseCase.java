package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.model.BookingSeriesRequest;

import java.util.List;
import java.util.UUID;

public interface GetBookingSeriesRequestsUseCase {
    List<BookingSeriesRequest> getOpenRequests(UUID adminUserId);
    List<BookingSeriesRequest> getAllRequests(UUID adminUserId);
    List<BookingSeriesRequest> getRequestsByUser(UUID userId);
}