package de.hallenbelegung.adapters.in.api.dto;

import java.util.List;
import java.util.UUID;

public record SeriesStatisticsDetailDTO(
        UUID bookingSeriesId,
        String title,
        String hallName,
        String responsibleUserName,
        long totalAppointments,
        long conductedAppointments,
        long cancelledAppointments,
        long totalParticipants,
        double averageParticipants,
        List<SeriesOccurrenceDTO> occurrences
) {
}
