package de.hallenbelegung.adapters.in.api.rest;

import de.hallenbelegung.adapters.in.api.dto.BlockedTimeDTO;
import de.hallenbelegung.adapters.in.api.dto.EmptyResponseDTO;
import de.hallenbelegung.application.domain.exception.UnauthorizedException;
import de.hallenbelegung.application.domain.exception.ValidationException;
import de.hallenbelegung.application.domain.model.BlockedTime;
import de.hallenbelegung.application.domain.model.BlockedTimeType;
import de.hallenbelegung.application.domain.model.Hall;
import de.hallenbelegung.application.domain.model.HallType;
import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.in.CreateBlockedTimeUseCase;
import de.hallenbelegung.application.domain.port.in.DeleteBlockedTimeUseCase;
import de.hallenbelegung.application.domain.port.in.GetBlockedTimeUseCase;
import de.hallenbelegung.application.domain.port.in.GetCurrentUserUseCase;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BlockedTimeResourceTest {

    private GetCurrentUserUseCase getCurrentUserUseCase;
    private GetBlockedTimeUseCase getBlockedTimeUseCase;
    private CreateBlockedTimeUseCase createBlockedTimeUseCase;
    private DeleteBlockedTimeUseCase deleteBlockedTimeUseCase;

    private BlockedTimeResource resource;

    @BeforeEach
    void setUp() {
        getCurrentUserUseCase = mock(GetCurrentUserUseCase.class);
        getBlockedTimeUseCase = mock(GetBlockedTimeUseCase.class);
        createBlockedTimeUseCase = mock(CreateBlockedTimeUseCase.class);
        deleteBlockedTimeUseCase = mock(DeleteBlockedTimeUseCase.class);

        resource = new BlockedTimeResource(
                getCurrentUserUseCase,
                getBlockedTimeUseCase,
                createBlockedTimeUseCase,
                deleteBlockedTimeUseCase
        );
    }

    private User admin(UUID id) {
        return new User(id, "Admin", "User", "admin@example.com", "hash", Role.ADMIN, true, Instant.now(), Instant.now());
    }

    private Hall hall(UUID id) {
        return new Hall(id, "Halle A", "desc", true, Instant.now(), Instant.now(), HallType.PART_SMALL);
    }

    private BlockedTime blockedTime(UUID id, Hall hall, User admin) {
        return new BlockedTime(
                id,
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
    }

    @Test
    void getAll_requires_cookie_and_maps_result() {
        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> resource.getAll(" ")
        );
        assertEquals("Missing session cookie", ex.getMessage());

        UUID adminId = UUID.randomUUID();
        User admin = admin(adminId);
        Hall hall = hall(UUID.randomUUID());
        BlockedTime blocked = blockedTime(UUID.randomUUID(), hall, admin);

        when(getCurrentUserUseCase.getCurrentUser("sess")).thenReturn(admin);
        when(getBlockedTimeUseCase.getAll(adminId)).thenReturn(List.of(blocked));

        List<BlockedTimeDTO> result = resource.getAll("sess");

        assertEquals(1, result.size());
        assertEquals(blocked.getId(), result.get(0).id());
        assertEquals(hall.getId(), result.get(0).hallId());
    }

    @Test
    void create_validates_fields_and_returns_created() {
        UUID adminId = UUID.randomUUID();
        User admin = admin(adminId);
        UUID hallId = UUID.randomUUID();

        when(getCurrentUserUseCase.getCurrentUser("sess")).thenReturn(admin);

        ValidationException ex1 = assertThrows(ValidationException.class, () -> resource.create(null, "sess"));
        assertEquals("Request body is required", ex1.getMessage());

        BlockedTimeDTO missingHall = new BlockedTimeDTO(
                null,
                "Wartung",
                LocalDateTime.of(2026, 5, 4, 8, 0),
                LocalDateTime.of(2026, 5, 4, 10, 0),
                null,
                null
        );
        ValidationException ex2 = assertThrows(ValidationException.class, () -> resource.create(missingHall, "sess"));
        assertEquals("hallId is required", ex2.getMessage());

        BlockedTimeDTO missingStart = new BlockedTimeDTO(null, "W", null, LocalDateTime.now(), hallId, null);
        ValidationException ex3 = assertThrows(ValidationException.class, () -> resource.create(missingStart, "sess"));
        assertEquals("startDateTime is required", ex3.getMessage());

        BlockedTimeDTO missingEnd = new BlockedTimeDTO(null, "W", LocalDateTime.now(), null, hallId, null);
        ValidationException ex4 = assertThrows(ValidationException.class, () -> resource.create(missingEnd, "sess"));
        assertEquals("endDateTime is required", ex4.getMessage());

        BlockedTimeDTO valid = new BlockedTimeDTO(
                null,
                "Wartung",
                LocalDateTime.of(2026, 5, 4, 8, 0),
                LocalDateTime.of(2026, 5, 4, 10, 0),
                hallId,
                null
        );

        Response response = resource.create(valid, "sess");

        assertEquals(201, response.getStatus());
        assertInstanceOf(EmptyResponseDTO.class, response.getEntity());
        verify(createBlockedTimeUseCase).create(hallId, "Wartung", valid.startDateTime(), valid.endDateTime(), adminId);
    }

    @Test
    void delete_delegates_to_use_case() {
        UUID blockedId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        when(getCurrentUserUseCase.getCurrentUser("sess")).thenReturn(admin(adminId));

        Response response = resource.delete(blockedId, "sess");

        assertEquals(200, response.getStatus());
        assertInstanceOf(EmptyResponseDTO.class, response.getEntity());
        verify(deleteBlockedTimeUseCase).delete(blockedId, adminId);
    }
}

