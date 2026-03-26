package de.hallenbelegung.adapters.in.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record BookingDTO(
        UUID id,
        String title,
        String description,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        UUID hallId,
        String hallName,
        String status,
        String responsibleUserName,
        Integer participantCount,
        String feedbackComment,
        boolean canViewFeedback,
        boolean canEdit,
        boolean canCancel
) {
}
