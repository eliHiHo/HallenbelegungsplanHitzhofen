package de.hallenbelegung.application.domain.port.in;

public interface RejectBookingSeriesRequestUseCase {
    void reject(Long adminUserId, Long bookingSeriesRequestId, String reason);
}