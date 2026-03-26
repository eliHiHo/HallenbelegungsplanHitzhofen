package de.hallenbelegung.adapters.in.api.dto;

import java.util.UUID;

public record SeriesUsageDTO(
        UUID bookingSeriesId,
        String title,
        long bookingCount
) {
}
