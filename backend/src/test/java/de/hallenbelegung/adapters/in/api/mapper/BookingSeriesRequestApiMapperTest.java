package de.hallenbelegung.adapters.in.api.mapper;

import de.hallenbelegung.adapters.in.api.dto.BookingSeriesApproveResultDTO;
import de.hallenbelegung.adapters.in.api.dto.BookingSeriesRequestDTO;
import de.hallenbelegung.application.domain.model.BookingRequestStatus;
import de.hallenbelegung.application.domain.model.BookingSeriesRequest;
import de.hallenbelegung.application.domain.model.Hall;
import de.hallenbelegung.application.domain.model.HallType;
import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.view.BookingSeriesApproveResult;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookingSeriesRequestApiMapperTest {

    @Test
    void toDTO_maps_request_and_approve_result() {
        Hall hall = new Hall(UUID.randomUUID(), "Halle A", "desc", true, Instant.now(), Instant.now(), HallType.PART_SMALL);
        User requester = new User(UUID.randomUUID(), "Max", "Mustermann", "max@example.com", "hash", Role.CLUB_REPRESENTATIVE, true, Instant.now(), Instant.now());
        BookingSeriesRequest request = new BookingSeriesRequest(
                UUID.randomUUID(),
                "Serie Anfrage",
                "desc",
                DayOfWeek.MONDAY,
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 1),
                BookingRequestStatus.PENDING,
                null,
                hall,
                requester,
                null,
                Instant.now(),
                Instant.now(),
                null
        );

        BookingSeriesRequestDTO dto = BookingSeriesRequestApiMapper.toDTO(request);
        assertEquals(request.getId(), dto.id());
        assertEquals("PENDING", dto.status());

        BookingSeriesApproveResult result = new BookingSeriesApproveResult(
                List.of(UUID.randomUUID()),
                List.of(LocalDate.of(2026, 5, 5))
        );
        BookingSeriesApproveResultDTO resultDTO = BookingSeriesRequestApiMapper.toDTO(result);
        assertEquals(1, resultDTO.createdBookingIds().size());
        assertEquals(1, resultDTO.skippedOccurrences().size());
    }
}

