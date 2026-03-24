package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.model.BookingRequest;

import java.util.List;
import java.util.UUID;

public interface GetBookingRequestsUseCase {
    List<BookingRequest> getOpenRequests(UUID adminUserId);
    List<BookingRequest> getAllRequests(UUID adminUserId);
    List<BookingRequest> getRequestsByUser(UUID userId);
}