package de.hallenbelegung.application.domain.port.in;

public interface ApproveBookingRequestUseCase {

    void approve(Long adminUserId, Long bookingRequestId);
}