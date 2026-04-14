package de.hallenbelegung.adapters.in.api.mapper;

import de.hallenbelegung.adapters.in.api.dto.UserDTO;
import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.model.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserApiMapperTest {

    @Test
    void toDTO_maps_all_fields() {
        User user = new User(UUID.randomUUID(), "Max", "Mustermann", "max@example.com", "hash", Role.ADMIN, true, Instant.now(), Instant.now());

        UserDTO dto = UserApiMapper.toDTO(user);

        assertEquals(user.getId(), dto.id());
        assertEquals("Max Mustermann", dto.fullName());
        assertEquals("ADMIN", dto.role());
    }
}

