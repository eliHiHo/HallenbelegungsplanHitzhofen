package de.hallenbelegung.application.domain.port.in;

import java.util.UUID;

public interface UpdateBookingFeedbackUseCase {

    void updateFeedback(UUID bookingId,
                        Integer participantCount,
                        String comment,
                        UUID userId);
}