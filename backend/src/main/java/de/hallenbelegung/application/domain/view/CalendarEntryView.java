package de.hallenbelegung.application.domain.view;

import java.time.LocalDateTime;

public record CalendarEntryView(
        Long id,
        CalendarEntryType type,
        String title,
        String description,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        Long hallId,
        String hallName,
        String status,
        boolean ownEntry
) {
}