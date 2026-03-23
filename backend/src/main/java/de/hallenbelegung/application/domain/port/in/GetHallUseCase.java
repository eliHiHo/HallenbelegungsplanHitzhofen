package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.adapters.in.api.dto.CalendarWeekDTO;
import de.hallenbelegung.adapters.in.api.dto.HallDTO;

import java.time.LocalDate;
import java.util.List;


public interface GetHallUseCase {

    List<HallDTO> getAll();
}
