package de.hallenbelegung.adapters.in.api.mapper;

import de.hallenbelegung.adapters.in.api.dto.BookingSeriesApproveResultDTO;
import de.hallenbelegung.adapters.in.api.dto.BookingSeriesRequestDTO;
import de.hallenbelegung.application.domain.model.BookingSeriesRequest;
import de.hallenbelegung.application.domain.view.BookingSeriesApproveResult;

public class BookingSeriesRequestApiMapper {

    public static BookingSeriesRequestDTO toDTO(BookingSeriesRequest r) {
        return new BookingSeriesRequestDTO(
                r.getId(),
                r.getTitle(),
                r.getDescription(),
                r.getWeekday(),
                r.getStartTime(),
                r.getEndTime(),
                r.getStartDate(),
                r.getEndDate(),
                r.getHall().getId(),
                r.getHall().getName(),
                r.getRequestedBy().getFullName(),
                r.getStatus().name(),
                r.getRejectionReason()
        );
    }

    public static BookingSeriesApproveResultDTO toDTO(BookingSeriesApproveResult r) {
        return new BookingSeriesApproveResultDTO(r.createdBookingIds, r.skippedOccurrences);
    }
}
