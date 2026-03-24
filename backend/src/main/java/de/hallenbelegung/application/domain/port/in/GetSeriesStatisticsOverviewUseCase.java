package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.view.SeriesStatisticsOverviewView;

import java.time.LocalDate;
import java.util.List;

public interface GetSeriesStatisticsOverviewUseCase {
    List<SeriesStatisticsOverviewView> getSeriesStatisticsOverview(Long currentUserId, LocalDate from, LocalDate to);
}