package de.hallenbelegung.adapters.in.api.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record BookingSeriesDTO(
        UUID id,
        String title,
        String description,
        DayOfWeek weekday,
        LocalTime startTime,
        LocalTime endTime,
        LocalDate startDate,
        LocalDate endDate,
        UUID hallId,
        String hallName,
        String status,
        String responsibleUserName
) {
}
