package de.hallenbelegung.application.domain.port.in;

import java.time.LocalDateTime;

public interface CreateBookingRequestUseCase {
    Long create(Long userId,
                Long hallId,
                String title,
                String description,
                LocalDateTime startTime,
                LocalDateTime endTime);
}