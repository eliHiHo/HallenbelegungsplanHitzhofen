package de.hallenbelegung.application.domain.port.in;

import java.time.LocalDateTime;
import java.util.UUID;

public interface CreateBookingRequestUseCase {
    UUID create(UUID userId,
                UUID hallId,
                String title,
                String description,
                LocalDateTime startTime,
                LocalDateTime endTime);
}