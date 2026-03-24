package de.hallenbelegung.application.domain.port.in;

import java.util.UUID;

public interface RejectBookingRequestUseCase {

    void reject(UUID adminUserId, UUID bookingRequestId, String reason);
}
