package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.model.Booking;

import java.time.LocalDateTime;
import java.util.UUID;

public interface UpdateBookingUseCase {

    Booking update(UUID currentUserId,
                   UUID bookingId,
                   UUID hallId,
                   String title,
                   String description,
                   LocalDateTime startTime,
                   LocalDateTime endTime);
}