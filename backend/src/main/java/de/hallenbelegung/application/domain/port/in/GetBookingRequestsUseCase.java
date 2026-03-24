package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.model.BookingRequest;

import java.util.List;

public interface GetBookingRequestsUseCase {
    List<BookingRequest> getOpenRequests(Long adminUserId);
    List<BookingRequest> getRequestsByUser(Long userId);
}
