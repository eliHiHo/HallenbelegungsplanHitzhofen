package de.hallenbelegung.adapters.in.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record BlockedTimeDTO(
        UUID id,
        String reason,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        UUID hallId,
        String hallName
) {
}
