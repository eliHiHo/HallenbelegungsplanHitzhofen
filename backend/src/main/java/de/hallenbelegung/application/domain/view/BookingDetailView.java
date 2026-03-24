package de.hallenbelegung.application.domain.view;

import java.time.LocalDateTime;

public record BookingDetailView(
        java.util.UUID id,
        String title,
        String description,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        java.util.UUID hallId,
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
