package de.hallenbelegung.adapters.in.api.mapper;

import de.hallenbelegung.adapters.in.api.dto.BookingDTO;
import de.hallenbelegung.application.domain.view.BookingDetailView;

public class BookingApiMapper {

    public static BookingDTO toDTO(BookingDetailView v) {
        return new BookingDTO(
                v.id(),
                v.title(),
                v.description(),
                v.startDateTime(),
                v.endDateTime(),
                v.hallId(),
                v.hallName(),
                v.status(),
                v.responsibleUserName(),
                v.participantCount(),
                v.feedbackComment(),
                v.canViewFeedback(),
                v.canEdit(),
                v.canCancel()
        );
    }
}
