package de.hallenbelegung.application.domain.port.in;

public interface RejectBookingRequestUseCase {

    void reject(Long bookingRequestId, Long adminUserId, String reason);
}
