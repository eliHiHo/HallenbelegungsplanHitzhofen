package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.view.CalendarDayView;

import java.time.LocalDate;
import java.util.UUID;

public interface GetCalendarDayUseCase {

    CalendarDayView getDay(LocalDate day, UUID userId);
}
