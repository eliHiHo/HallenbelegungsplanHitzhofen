package de.hallenbelegung.adapters.in.api.mapper;

import de.hallenbelegung.adapters.in.api.dto.BookingDTO;
import de.hallenbelegung.application.domain.view.BookingDetailView;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookingApiMapperTest {

    @Test
    void toDTO_maps_all_fields() {
        UUID bookingId = UUID.randomUUID();
        UUID hallId = UUID.randomUUID();
        BookingDetailView view = new BookingDetailView(
                bookingId,
                "Training",
                "desc",
                LocalDateTime.of(2026, 5, 4, 10, 0),
                LocalDateTime.of(2026, 5, 4, 11, 0),
                hallId,
                "Halle A",
                "APPROVED",
                "Max Mustermann",
                12,
                "ok",
                true,
                true,
                true
        );

        BookingDTO dto = BookingApiMapper.toDTO(view);

        assertEquals(bookingId, dto.id());
        assertEquals(hallId, dto.hallId());
        assertEquals(12, dto.participantCount());
        assertEquals(true, dto.canCancel());
    }
}

