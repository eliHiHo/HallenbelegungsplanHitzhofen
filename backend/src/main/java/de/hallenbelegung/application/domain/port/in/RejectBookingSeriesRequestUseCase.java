package de.hallenbelegung.application.domain.port.in;

public interface RejectBookingSeriesRequestUseCase {
    void reject(Long bookingSeriesRequestId, Long adminUserId, String reason);
}