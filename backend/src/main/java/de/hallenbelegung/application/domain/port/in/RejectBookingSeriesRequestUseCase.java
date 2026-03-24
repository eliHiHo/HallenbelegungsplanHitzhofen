package de.hallenbelegung.application.domain.port.in;

import java.util.UUID;

public interface RejectBookingSeriesRequestUseCase {
    void reject(UUID adminUserId, UUID bookingSeriesRequestId, String reason);
}