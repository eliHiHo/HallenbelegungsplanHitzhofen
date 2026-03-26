package de.hallenbelegung.adapters.out.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BavariaPublicHolidayAdapterTest {

    @Test
    public void testFixedHolidaysPresent() {
        BavariaPublicHolidayAdapter adapter = new BavariaPublicHolidayAdapter();
        int year = 2025;
        List<LocalDate> holidays = adapter.findHolidays(LocalDate.of(year,1,1), LocalDate.of(year,12,31));

        assertTrue(holidays.contains(LocalDate.of(year,1,1)), "Neujahr present");
        assertTrue(holidays.contains(LocalDate.of(year,1,6)), "Heilige Drei Könige present");
        assertTrue(holidays.contains(LocalDate.of(year,12,25)), "1. Weihnachtstag present");
        assertTrue(holidays.contains(LocalDate.of(year,12,26)), "2. Weihnachtstag present");

        // Karfreitag should be before Ostermontag: find any Good Friday / Easter Monday pair
        LocalDate goodFriday = holidays.stream().filter(d -> d.getMonthValue() >= 3 && d.getMonthValue() <= 4 && d.getDayOfWeek().getValue() == 5).findFirst().orElse(null);
        LocalDate easterMonday = holidays.stream().filter(d -> d.getMonthValue() >= 3 && d.getMonthValue() <= 6 && d.getDayOfWeek().getValue() == 1).findFirst().orElse(null);
        assertNotNull(goodFriday, "Good Friday present");
        assertNotNull(easterMonday, "Easter Monday present");
        assertTrue(goodFriday.isBefore(easterMonday), "Good Friday before Easter Monday");
    }
}
