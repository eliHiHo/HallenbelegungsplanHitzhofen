package de.hallenbelegung.adapters.in.api.mapper;

import de.hallenbelegung.adapters.in.api.dto.BookingRequestDTO;
import de.hallenbelegung.application.domain.model.BookingRequest;

public class BookingRequestApiMapper {

    public static BookingRequestDTO toDTO(BookingRequest r) {
        return new BookingRequestDTO(
                r.getId(),
                r.getTitle(),
                r.getDescription(),
                r.getStartAt(),
                r.getEndAt(),
                r.getHall().getId(),
                r.getHall().getName(),
                r.getRequestedBy().getFullName(),
                r.getStatus().name(),
                r.getRejectionReason()
        );
    }
}
