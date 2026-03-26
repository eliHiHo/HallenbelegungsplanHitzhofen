package de.hallenbelegung.adapters.out.service;

import de.hallenbelegung.application.domain.port.out.PublicHolidayPort;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class BavariaPublicHolidayAdapter implements PublicHolidayPort {

    @Override
    public List<LocalDate> findHolidays(LocalDate start, LocalDate end) {
        List<LocalDate> holidays = new ArrayList<>();
        if (start == null || end == null || start.isAfter(end)) return holidays;

        LocalDate cur = start.withDayOfYear(1);
        int startYear = start.getYear();
        int endYear = end.getYear();

        for (int y = startYear; y <= endYear; y++) {
            List<LocalDate> yearHolidays = holidaysForYear(y);
            for (LocalDate d : yearHolidays) {
                if ((d.isEqual(start) || d.isAfter(start)) && (d.isEqual(end) || d.isBefore(end))) {
                    holidays.add(d);
                }
            }
        }

        return holidays;
    }

    private List<LocalDate> holidaysForYear(int year) {
        List<LocalDate> list = new ArrayList<>();
        // Fixed-date holidays
        list.add(LocalDate.of(year, Month.JANUARY, 1)); // Neujahr
        list.add(LocalDate.of(year, Month.JANUARY, 6)); // Heilige Drei Könige
        list.add(LocalDate.of(year, Month.MAY, 1)); // Tag der Arbeit
        list.add(LocalDate.of(year, Month.AUGUST, 15)); // Mariä Himmelfahrt
        list.add(LocalDate.of(year, Month.OCTOBER, 3)); // Tag der Deutschen Einheit
        list.add(LocalDate.of(year, Month.NOVEMBER, 1)); // Allerheiligen
        list.add(LocalDate.of(year, Month.DECEMBER, 25)); // 1. Weihnachtstag
        list.add(LocalDate.of(year, Month.DECEMBER, 26)); // 2. Weihnachtstag

        // Movable feasts: compute Easter
        LocalDate easter = easterSunday(year);
        list.add(easter.minusDays(2)); // Karfreitag
        list.add(easter.plusDays(1)); // Ostermontag
        list.add(easter.plusDays(39)); // Christi Himmelfahrt
        list.add(easter.plusDays(50)); // Pfingstmontag
        list.add(easter.plusDays(60)); // Fronleichnam (approx: 60 days after Easter)

        return list;
    }

    // Computus: Anonymous Gregorian algorithm
    private LocalDate easterSunday(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31; // 3=March, 4=April
        int day = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(year, month, day);
    }
}
