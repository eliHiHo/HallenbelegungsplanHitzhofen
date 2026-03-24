package de.hallenbelegung.application.domain.port.in;

import java.util.UUID;

public interface CancelBookingUseCase {

    void cancel(UUID currentUserId, UUID bookingId, String cancellationReason);
}