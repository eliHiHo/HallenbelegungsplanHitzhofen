package de.hallenbelegung.application.domain.port.in;


public interface CancelBookingSeriesOccurrenceUseCase {
    void cancelSingleOccurrence(Long currentUserId,
                                Long bookingSeriesId,
                                Long bookingId,
                                String cancellationReason);
}