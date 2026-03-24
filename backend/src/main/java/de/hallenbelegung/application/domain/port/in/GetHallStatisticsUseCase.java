package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.view.HallStatisticsView;

import java.time.LocalDate;
import java.util.List;

public interface GetHallStatisticsUseCase {
    List<HallStatisticsView> getHallStatistics(Long currentUserId, LocalDate from, LocalDate to);
}