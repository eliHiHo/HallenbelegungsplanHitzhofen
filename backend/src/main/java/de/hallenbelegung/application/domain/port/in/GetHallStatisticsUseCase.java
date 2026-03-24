package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.view.HallStatisticsView;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface GetHallStatisticsUseCase {
    List<HallStatisticsView> getHallStatistics(UUID currentUserId, LocalDate from, LocalDate to);
}