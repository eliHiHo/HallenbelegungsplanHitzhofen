package de.hallenbelegung.adapters.in.api.dto;

import java.time.LocalDate;
import java.util.List;

public record CalendarDayDTO(
        LocalDate day,
        List<CalendarEntryDTO> entries
) {
}
