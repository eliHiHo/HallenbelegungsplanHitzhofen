package de.hallenbelegung.adapters.in.api.mapper;

import de.hallenbelegung.adapters.in.api.dto.CalendarDayDTO;
import de.hallenbelegung.adapters.in.api.dto.CalendarEntryDTO;
import de.hallenbelegung.adapters.in.api.dto.CalendarWeekDTO;
import de.hallenbelegung.application.domain.view.CalendarDayView;
import de.hallenbelegung.application.domain.view.CalendarEntryType;
import de.hallenbelegung.application.domain.view.CalendarEntryView;
import de.hallenbelegung.application.domain.view.CalendarWeekView;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalendarApiMapperTest {

    @Test
    void toDTO_maps_week_day_and_entry() {
        CalendarEntryView entry = new CalendarEntryView(
                UUID.randomUUID(),
                CalendarEntryType.BOOKING,
                "Training",
                "desc",
                LocalDateTime.of(2026, 5, 4, 10, 0),
                LocalDateTime.of(2026, 5, 4, 11, 0),
                UUID.randomUUID(),
                "Halle A",
                "Max Mustermann",
                "APPROVED",
                true
        );

        CalendarWeekDTO week = CalendarApiMapper.toDTO(new CalendarWeekView(LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 10), List.of(entry)));
        CalendarDayDTO day = CalendarApiMapper.toDTO(new CalendarDayView(LocalDate.of(2026, 5, 4), List.of(entry)));
        CalendarEntryDTO entryDTO = CalendarApiMapper.entryToDTO(entry);

        assertEquals(1, week.entries().size());
        assertEquals(1, day.entries().size());
        assertEquals("BOOKING", entryDTO.type());
        assertEquals(entry.id(), entryDTO.id());
    }
}

