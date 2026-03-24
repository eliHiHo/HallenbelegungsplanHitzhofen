package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.view.CalendarDayView;

import java.time.LocalDate;

public interface GetCalendarDayUseCase {

    CalendarDayView getDay(LocalDate day, Long userId);
}
