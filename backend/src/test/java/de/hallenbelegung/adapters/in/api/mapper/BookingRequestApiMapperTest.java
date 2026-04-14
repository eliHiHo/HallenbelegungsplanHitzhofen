package de.hallenbelegung.adapters.in.api.mapper;

import de.hallenbelegung.adapters.in.api.dto.BookingRequestDTO;
import de.hallenbelegung.application.domain.model.BookingRequest;
import de.hallenbelegung.application.domain.model.BookingRequestStatus;
import de.hallenbelegung.application.domain.model.Hall;
import de.hallenbelegung.application.domain.model.HallType;
import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.model.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookingRequestApiMapperTest {

    @Test
    void toDTO_maps_all_fields() {
        Hall hall = new Hall(UUID.randomUUID(), "Halle A", "desc", true, Instant.now(), Instant.now(), HallType.PART_SMALL);
        User requester = new User(UUID.randomUUID(), "Max", "Mustermann", "max@example.com", "hash", Role.CLUB_REPRESENTATIVE, true, Instant.now(), Instant.now());
        BookingRequest request = new BookingRequest(
                UUID.randomUUID(),
                "Anfrage",
                "desc",
                LocalDateTime.of(2026, 5, 4, 10, 0),
                LocalDateTime.of(2026, 5, 4, 11, 0),
                BookingRequestStatus.PENDING,
                null,
                hall,
                requester,
                null,
                Instant.now(),
                Instant.now(),
                null
        );

        BookingRequestDTO dto = BookingRequestApiMapper.toDTO(request);

        assertEquals(request.getId(), dto.id());
        assertEquals(hall.getName(), dto.hallName());
        assertEquals("PENDING", dto.status());
    }
}

