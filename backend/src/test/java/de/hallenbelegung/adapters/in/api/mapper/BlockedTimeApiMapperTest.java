package de.hallenbelegung.adapters.in.api.mapper;

import de.hallenbelegung.adapters.in.api.dto.BlockedTimeDTO;
import de.hallenbelegung.application.domain.model.BlockedTime;
import de.hallenbelegung.application.domain.model.BlockedTimeType;
import de.hallenbelegung.application.domain.model.Hall;
import de.hallenbelegung.application.domain.model.HallType;
import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.model.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockedTimeApiMapperTest {

    @Test
    void toDTO_maps_all_fields() {
        Hall hall = new Hall(UUID.randomUUID(), "Halle A", "desc", true, Instant.now(), Instant.now(), HallType.PART_SMALL);
        User admin = new User(UUID.randomUUID(), "Admin", "User", "admin@example.com", "hash", Role.ADMIN, true, Instant.now(), Instant.now());
        BlockedTime blocked = new BlockedTime(
                UUID.randomUUID(),
                "Wartung",
                LocalDateTime.of(2026, 5, 4, 8, 0),
                LocalDateTime.of(2026, 5, 4, 10, 0),
                BlockedTimeType.MANUAL,
                hall,
                admin,
                admin,
                Instant.now(),
                Instant.now()
        );

        BlockedTimeDTO dto = BlockedTimeApiMapper.toDTO(blocked);

        assertEquals(blocked.getId(), dto.id());
        assertEquals("Wartung", dto.reason());
        assertEquals(hall.getId(), dto.hallId());
    }
}

