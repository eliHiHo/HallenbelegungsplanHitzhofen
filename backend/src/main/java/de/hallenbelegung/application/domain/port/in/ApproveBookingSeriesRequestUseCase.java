package de.hallenbelegung.application.domain.port.in;

public interface ApproveBookingSeriesRequestUseCase {
    void approve(Long bookingSeriesRequestId, Long adminUserId);
}