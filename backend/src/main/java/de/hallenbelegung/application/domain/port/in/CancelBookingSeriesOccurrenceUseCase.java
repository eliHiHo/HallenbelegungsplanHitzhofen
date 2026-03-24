package de.hallenbelegung.application.domain.port.in;

public interface CancelBookingSeriesOccurrenceUseCase {
    void cancelSingleOccurrence(Long bookingSeriesId,
                                Long bookingId,
                                Long userId,
                                String cancellationReason);
}