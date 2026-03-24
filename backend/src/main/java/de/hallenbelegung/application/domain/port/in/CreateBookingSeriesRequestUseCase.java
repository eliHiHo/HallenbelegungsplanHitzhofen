package de.hallenbelegung.application.domain.port.in;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public interface CreateBookingSeriesRequestUseCase {
    UUID create(UUID userId,
                UUID hallId,
                String title,
                String description,
                DayOfWeek weekday,
                LocalTime startTime,
                LocalTime endTime,
                LocalDate startDate,
                LocalDate endDate);
}