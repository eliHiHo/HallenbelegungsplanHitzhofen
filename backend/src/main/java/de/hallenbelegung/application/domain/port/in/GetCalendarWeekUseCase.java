package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.view.CalendarWeekView;

import java.time.LocalDate;
import java.util.UUID;

public interface GetCalendarWeekUseCase {

    CalendarWeekView getWeek(LocalDate weekStart, UUID userId);
}