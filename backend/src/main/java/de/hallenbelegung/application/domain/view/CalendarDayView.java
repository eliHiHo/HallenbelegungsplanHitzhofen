package de.hallenbelegung.application.domain.view;

import java.time.LocalDate;
import java.util.List;

public record CalendarDayView(
        LocalDate day,
        List<CalendarEntryView> entries
) {
}