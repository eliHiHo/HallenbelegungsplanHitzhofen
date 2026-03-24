package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.model.Booking;
import de.hallenbelegung.application.domain.view.BookingDetailView;

public interface GetBookingUseCase {
    BookingDetailView getById(Long currentUserId, Long bookingId);
}
