package de.hallenbelegung.application.domain.port.out;

import java.time.LocalDate;
import java.util.List;

public interface PublicHolidayPort {
    List<LocalDate> findHolidays(LocalDate start, LocalDate end);
}
