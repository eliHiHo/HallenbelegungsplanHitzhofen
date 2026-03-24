package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.view.SeriesStatisticsDetailView;

import java.time.LocalDate;
import java.util.UUID;

public interface GetSeriesStatisticsDetailUseCase {
    SeriesStatisticsDetailView getSeriesStatisticsDetail(UUID currentUserId, UUID bookingSeriesId, LocalDate from, LocalDate to);
}