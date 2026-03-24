package de.hallenbelegung.application.domain.view;

import java.time.LocalDate;
import java.util.List;

public record CalendarWeekView(
        LocalDate weekStart,
        LocalDate weekEnd,
        List<CalendarEntryView> entries
) {
}
