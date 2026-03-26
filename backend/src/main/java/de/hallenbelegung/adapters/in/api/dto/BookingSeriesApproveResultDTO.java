package de.hallenbelegung.adapters.in.api.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BookingSeriesApproveResultDTO(
        List<UUID> createdBookingIds,
        List<LocalDate> skippedOccurrences
) {
}
