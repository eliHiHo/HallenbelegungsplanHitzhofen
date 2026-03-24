package de.hallenbelegung.application.domain.port.in;

public interface CancelBookingUseCase {

    void cancel(Long currentUserId, Long bookingId, String cancellationReason);
}