package de.hallenbelegung.application.domain.port.in;

public interface CancelBookingSeriesUseCase {
    void cancelSeries(Long currentUserId, Long bookingSeriesId, String cancellationReason);
}