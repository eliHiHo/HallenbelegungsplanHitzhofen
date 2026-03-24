package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.model.Booking;

import java.util.List;
import java.util.UUID;

public interface GetUserBookingsUseCase {
    List<Booking> getBookingsByUser(UUID userId);
}
