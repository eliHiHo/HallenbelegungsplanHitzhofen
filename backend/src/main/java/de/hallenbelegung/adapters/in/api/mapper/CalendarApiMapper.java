package de.hallenbelegung.adapters.in.api.mapper;

import de.hallenbelegung.adapters.in.api.dto.CalendarDayDTO;
import de.hallenbelegung.adapters.in.api.dto.CalendarEntryDTO;
import de.hallenbelegung.adapters.in.api.dto.CalendarWeekDTO;
import de.hallenbelegung.application.domain.view.CalendarDayView;
import de.hallenbelegung.application.domain.view.CalendarEntryView;
import de.hallenbelegung.application.domain.view.CalendarWeekView;

import java.util.stream.Collectors;

public class CalendarApiMapper {

    public static CalendarWeekDTO toDTO(CalendarWeekView view) {
        return new CalendarWeekDTO(
                view.weekStart(),
                view.weekEnd(),
                view.entries().stream().map(CalendarApiMapper::entryToDTO).collect(Collectors.toList())
        );
    }

    public static CalendarDayDTO toDTO(CalendarDayView view) {
        return new CalendarDayDTO(
                view.day(),
                view.entries().stream().map(CalendarApiMapper::entryToDTO).collect(Collectors.toList())
        );
    }

    public static CalendarEntryDTO entryToDTO(CalendarEntryView e) {
        return new CalendarEntryDTO(
                e.id(),
                e.type().name(),
                e.title(),
                e.description(),
                e.startDateTime(),
                e.endDateTime(),
                e.hallId(),
                e.hallName(),
                e.responsibleUserName(),
                e.status(),
                e.ownEntry(),
                e.bookingSeriesId()
        );
    }
}
