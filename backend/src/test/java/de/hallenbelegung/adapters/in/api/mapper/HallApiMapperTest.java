package de.hallenbelegung.adapters.in.api.mapper;

import de.hallenbelegung.adapters.in.api.dto.HallDTO;
import de.hallenbelegung.application.domain.model.Hall;
import de.hallenbelegung.application.domain.model.HallType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HallApiMapperTest {

    @Test
    void toDTO_maps_all_fields() {
        Hall hall = new Hall(UUID.randomUUID(), "Halle A", "desc", true, Instant.now(), Instant.now(), HallType.FULL);

        HallDTO dto = HallApiMapper.toDTO(hall);

        assertEquals(hall.getId(), dto.id());
        assertEquals("FULL", dto.type());
        assertEquals(true, dto.active());
    }
}

