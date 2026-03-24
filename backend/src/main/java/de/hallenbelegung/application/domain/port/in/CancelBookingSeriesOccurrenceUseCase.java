package de.hallenbelegung.application.domain.port.in;


import java.util.UUID;

public interface CancelBookingSeriesOccurrenceUseCase {
    void cancelSingleOccurrence(UUID currentUserId,
                                UUID bookingSeriesId,
                                UUID bookingId,
                                String cancellationReason);
}