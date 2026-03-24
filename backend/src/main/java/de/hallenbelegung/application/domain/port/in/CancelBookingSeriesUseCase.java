package de.hallenbelegung.application.domain.port.in;

public interface CancelBookingSeriesUseCase {
    void cancelSeries(Long bookingSeriesId, Long userId, String cancellationReason);
}