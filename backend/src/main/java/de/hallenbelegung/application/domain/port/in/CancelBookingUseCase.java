package de.hallenbelegung.application.domain.port.in;

public interface CancelBookingUseCase {

    cancel(Long currentUserId, Long bookingId, String cancellationReason);
}