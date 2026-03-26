package de.hallenbelegung.adapters.in.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SeriesOccurrenceDTO(
        UUID bookingId,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        boolean cancelled,
        boolean conducted,
        Integer participantCount,
        String feedbackComment
) {
}
