package de.hallenbelegung.application.domain.port.in;

import java.util.UUID;

public interface ApproveBookingRequestUseCase {

    void approve(UUID adminUserId, UUID bookingRequestId);
}