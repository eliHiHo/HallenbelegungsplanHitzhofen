package de.hallenbelegung.application.domain.view;

import java.time.LocalDateTime;

public record BookingDetailView(
        Long id,
        String title,
        String description,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        Long hallId,
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
