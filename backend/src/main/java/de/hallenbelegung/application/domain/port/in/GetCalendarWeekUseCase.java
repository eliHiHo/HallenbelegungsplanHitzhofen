package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.view.CalendarWeekView;

import java.time.LocalDate;

public interface GetCalendarWeekUseCase {

    CalendarWeekView getWeek( LocalDate weekStart, Long userId);
}