package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.model.Booking;

import java.util.List;

public interface GetUserBookingsUseCase {
    List<Booking> getBookingsByUser(Long userId);
}
