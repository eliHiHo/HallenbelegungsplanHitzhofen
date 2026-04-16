package de.hallenbelegung.adapters.in.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CalendarEntryDTO(
        UUID id,
        String type,
        String title,
        String description,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        UUID hallId,
        String hallName,
        String responsibleUserName,
        String status,
        boolean ownEntry,
        UUID bookingSeriesId  // null wenn keine Serie
) {
}
