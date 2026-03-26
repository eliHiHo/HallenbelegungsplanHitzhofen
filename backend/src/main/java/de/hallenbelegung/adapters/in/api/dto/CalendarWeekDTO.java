package de.hallenbelegung.adapters.in.api.dto;

import java.time.LocalDate;
import java.util.List;

public record CalendarWeekDTO(
        LocalDate weekStart,
        LocalDate weekEnd,
        List<CalendarEntryDTO> entries
) {
}
