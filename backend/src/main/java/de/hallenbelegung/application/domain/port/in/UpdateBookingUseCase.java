package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.model.Booking;

import java.time.LocalDateTime;

public interface UpdateBookingUseCase {

    Booking update(Long currentUserId,
                   Long bookingId,
                   Long hallId,
                   String title,
                   String description,
                   LocalDateTime startTime,
                   LocalDateTime endTime);
}