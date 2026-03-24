package de.hallenbelegung.application.domain.port.in;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

public interface CreateBookingSeriesRequestUseCase {
    Long create(Long userId,
                Long hallId,
                String title,
                String description,
                DayOfWeek weekday,
                LocalTime startTime,
                LocalTime endTime,
                LocalDate startDate,
                LocalDate endDate);
}