package de.hallenbelegung.adapters.out.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BavariaPublicHolidayAdapterTest {

    private final BavariaPublicHolidayAdapter adapter = new BavariaPublicHolidayAdapter();

    @Test
    void findHolidays_returns_empty_for_null_or_invalid_range() {
        assertTrue(adapter.findHolidays(null, LocalDate.of(2026, 12, 31)).isEmpty());
        assertTrue(adapter.findHolidays(LocalDate.of(2026, 1, 1), null).isEmpty());
        assertTrue(adapter.findHolidays(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 31)).isEmpty());
    }

    @Test
    void findHolidays_returns_only_dates_inside_requested_window() {
        List<LocalDate> holidays = adapter.findHolidays(
                LocalDate.of(2026, 12, 24),
                LocalDate.of(2026, 12, 26)
        );

        assertEquals(2, holidays.size());
        assertTrue(holidays.contains(LocalDate.of(2026, 12, 25)));
        assertTrue(holidays.contains(LocalDate.of(2026, 12, 26)));
    }

    @Test
    void findHolidays_includes_expected_cross_year_fixed_holidays() {
        List<LocalDate> holidays = adapter.findHolidays(
                LocalDate.of(2025, 12, 31),
                LocalDate.of(2026, 1, 6)
        );

        assertEquals(2, holidays.size());
        assertTrue(holidays.contains(LocalDate.of(2026, 1, 1)));
        assertTrue(holidays.contains(LocalDate.of(2026, 1, 6)));
    }

    @Test
    void findHolidays_contains_known_movable_feasts_for_2026() {
        List<LocalDate> holidays = adapter.findHolidays(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31)
        );

        assertTrue(holidays.contains(LocalDate.of(2026, 4, 3)));   // Karfreitag
        assertTrue(holidays.contains(LocalDate.of(2026, 4, 6)));   // Ostermontag
        assertTrue(holidays.contains(LocalDate.of(2026, 5, 14)));  // Christi Himmelfahrt
        assertTrue(holidays.contains(LocalDate.of(2026, 5, 25)));  // Pfingstmontag
        assertTrue(holidays.contains(LocalDate.of(2026, 6, 4)));   // Fronleichnam
    }
}
