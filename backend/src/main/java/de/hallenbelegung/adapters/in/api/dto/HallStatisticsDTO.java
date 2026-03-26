package de.hallenbelegung.adapters.in.api.dto;

public record HallStatisticsDTO(
        String hallName,
        long bookingCount
) {
}
