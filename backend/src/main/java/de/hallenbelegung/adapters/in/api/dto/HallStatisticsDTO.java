package de.hallenbelegung.adapters.in.api.dto;

import java.util.List;
import java.util.UUID;

public record HallStatisticsDTO(
        UUID hallId,
        String hallName,
        long totalBookings,
        long cancelledBookings,
        long totalParticipants,
        double utilizationPercent,
        List<SeriesUsageDTO> topSeries
) {
}
