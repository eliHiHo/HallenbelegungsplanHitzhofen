package de.hallenbelegung.application.domain.port.in;

public interface UpdateBookingFeedbackUseCase {

    void updateFeedback(Long bookingId,
                        Integer participantCount,
                        String comment,
                        Long userId);
}