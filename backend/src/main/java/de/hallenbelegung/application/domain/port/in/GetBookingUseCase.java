package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.model.Booking;
import de.hallenbelegung.application.domain.view.BookingDetailView;

import java.util.UUID;

public interface GetBookingUseCase {
    BookingDetailView getById(UUID currentUserId, UUID bookingId);
}
