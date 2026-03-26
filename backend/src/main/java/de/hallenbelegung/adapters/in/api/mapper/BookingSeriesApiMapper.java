package de.hallenbelegung.adapters.in.api.mapper;

import de.hallenbelegung.adapters.in.api.dto.BookingSeriesDTO;
import de.hallenbelegung.application.domain.model.BookingSeries;

public class BookingSeriesApiMapper {

    public static BookingSeriesDTO toDTO(BookingSeries s) {
        return new BookingSeriesDTO(
                s.getId(),
                s.getTitle(),
                s.getDescription(),
                s.getWeekday(),
                s.getStartTime(),
                s.getEndTime(),
                s.getStartDate(),
                s.getEndDate(),
                s.getHall().getId(),
                s.getHall().getName(),
                s.getStatus().name(),
                s.getResponsibleUser() != null ? s.getResponsibleUser().getFullName() : null
        );
    }
}
