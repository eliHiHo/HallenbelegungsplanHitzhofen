package de.hallenbelegung.application.domain.port.in;

import java.util.UUID;

public interface CancelBookingSeriesUseCase {
    void cancelSeries(UUID currentUserId, UUID bookingSeriesId, String cancellationReason);
}