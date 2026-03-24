package de.hallenbelegung.application.domain.port.in;

public interface RejectBookingRequestUseCase {

    void reject(Long adminUserId, Long bookingRequestId, String reason);
}
