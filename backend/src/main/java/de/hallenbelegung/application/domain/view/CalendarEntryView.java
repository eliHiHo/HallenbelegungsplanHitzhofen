package de.hallenbelegung.application.domain.view;

import java.time.LocalDateTime;
import java.util.UUID;

public record CalendarEntryView(
        UUID id,
        CalendarEntryType type,
        String title,
        String description,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        UUID hallId,
        String hallName,
        String responsibleUserName,
        String status,
        boolean ownEntry,
        UUID bookingSeriesId  // null für nicht-serien-Buchungen, BLOCKED_TIME und Anfragen
) {
}