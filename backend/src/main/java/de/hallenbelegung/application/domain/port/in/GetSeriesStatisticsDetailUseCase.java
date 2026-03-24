package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.view.SeriesStatisticsDetailView;

import java.time.LocalDate;

public interface GetSeriesStatisticsDetailUseCase {
    SeriesStatisticsDetailView getSeriesStatisticsDetail(Long currentUserId, Long bookingSeriesId, LocalDate from, LocalDate to);
}