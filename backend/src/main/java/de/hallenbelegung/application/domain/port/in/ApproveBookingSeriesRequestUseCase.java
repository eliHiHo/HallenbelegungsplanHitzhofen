package de.hallenbelegung.application.domain.port.in;

import java.util.UUID;

public interface ApproveBookingSeriesRequestUseCase {
    void approve(UUID adminUserId, UUID bookingSeriesRequestId);
}