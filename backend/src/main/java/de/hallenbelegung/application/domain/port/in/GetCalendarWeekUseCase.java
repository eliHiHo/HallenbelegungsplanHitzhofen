package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.adapters.in.api.dto.CalendarWeekDTO;
import de.hallenbelegung.application.domain.view.CalendarWeekView;

import java.time.LocalDate;
import java.util.Map;

public interface GetCalendarWeekUseCase {

    CalendarWeekView getWeek( LocalDate weekStart, Long userId);
}