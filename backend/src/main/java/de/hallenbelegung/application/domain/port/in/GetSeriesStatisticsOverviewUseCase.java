package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.view.SeriesStatisticsOverviewView;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface GetSeriesStatisticsOverviewUseCase {
    List<SeriesStatisticsOverviewView> getSeriesStatisticsOverview(UUID currentUserId, LocalDate from, LocalDate to);
}