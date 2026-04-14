package de.hallenbelegung.adapters.in.api.mapper;

import de.hallenbelegung.adapters.in.api.dto.BookingSeriesDTO;
import de.hallenbelegung.application.domain.model.BookingSeries;
import de.hallenbelegung.application.domain.model.BookingSeriesStatus;
import de.hallenbelegung.application.domain.model.Hall;
import de.hallenbelegung.application.domain.model.HallType;
import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.model.User;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookingSeriesApiMapperTest {

    @Test
    void toDTO_maps_fields() {
        Hall hall = new Hall(UUID.randomUUID(), "Halle A", "desc", true, Instant.now(), Instant.now(), HallType.PART_SMALL);
        User owner = new User(UUID.randomUUID(), "Max", "Mustermann", "max@example.com", "hash", Role.CLUB_REPRESENTATIVE, true, Instant.now(), Instant.now());

        BookingSeries series = new BookingSeries(
                UUID.randomUUID(),
                "Serie",
                "desc",
                DayOfWeek.MONDAY,
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 1),
                BookingSeriesStatus.ACTIVE,
                hall,
                owner,
                owner,
                owner,
                null,
                Instant.now(),
                Instant.now(),
                null,
                null
        );

        BookingSeriesDTO dto = BookingSeriesApiMapper.toDTO(series);
        assertEquals(series.getId(), dto.id());
        assertEquals("Max Mustermann", dto.responsibleUserName());

        assertEquals("ACTIVE", dto.status());
    }
}

