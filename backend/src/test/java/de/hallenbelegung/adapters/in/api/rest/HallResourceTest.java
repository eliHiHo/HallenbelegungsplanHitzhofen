package de.hallenbelegung.adapters.in.api.rest;

import de.hallenbelegung.adapters.in.api.dto.HallDTO;
import de.hallenbelegung.application.domain.exception.UnauthorizedException;
import de.hallenbelegung.application.domain.model.Hall;
import de.hallenbelegung.application.domain.model.HallType;
import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.in.GetCurrentUserUseCase;
import de.hallenbelegung.application.domain.port.in.GetHallUseCase;
import de.hallenbelegung.application.domain.port.in.ManageHallUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HallResourceTest {

    private GetHallUseCase getHallUseCase;
    private ManageHallUseCase manageHallUseCase;
    private GetCurrentUserUseCase getCurrentUserUseCase;

    private HallResource resource;

    @BeforeEach
    void setUp() {
        getHallUseCase = mock(GetHallUseCase.class);
        manageHallUseCase = mock(ManageHallUseCase.class);
        getCurrentUserUseCase = mock(GetCurrentUserUseCase.class);

        resource = new HallResource(getHallUseCase, manageHallUseCase, getCurrentUserUseCase);
    }

    private Hall hall(boolean active, String name, HallType type) {
        return new Hall(UUID.randomUUID(), name, "desc", active, Instant.now(), Instant.now(), type);
    }

    private User admin() {
        return new User(UUID.randomUUID(), "Admin", "User", "admin@example.com", "hash", Role.ADMIN, true, Instant.now(), Instant.now());
    }

    @Test
    void getAll_without_includeInactive_returns_only_active() {
        Hall hall = hall(true, "Halle A", HallType.PART_SMALL);
        when(getHallUseCase.getAllActive()).thenReturn(List.of(hall));

        List<HallDTO> result = resource.getAll(false, null);

        assertEquals(1, result.size());
        assertEquals(hall.getId(), result.get(0).id());
        verify(getHallUseCase).getAllActive();
    }

    @Test
    void getAll_with_includeInactive_requires_cookie() {
        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> resource.getAll(true, "  "));
        assertEquals("Missing session cookie", ex.getMessage());
    }

    @Test
    void getAll_with_includeInactive_uses_manage_use_case() {
        User admin = admin();
        Hall active = hall(true, "Halle A", HallType.PART_SMALL);
        Hall inactive = hall(false, "Halle B", HallType.FULL);

        when(getCurrentUserUseCase.getCurrentUser("sess")).thenReturn(admin);
        when(manageHallUseCase.getAllIncludingInactive(admin.getId())).thenReturn(List.of(active, inactive));

        List<HallDTO> result = resource.getAll(true, "sess");

        assertEquals(2, result.size());
        assertEquals("PART_SMALL", result.get(0).type());
        assertEquals("FULL", result.get(1).type());
    }

    @Test
    void getById_without_includeInactive_uses_public_use_case() {
        Hall hall = hall(true, "Halle A", HallType.PART_SMALL);
        when(getHallUseCase.getById(hall.getId())).thenReturn(hall);

        HallDTO dto = resource.getById(hall.getId(), false, null);

        assertEquals(hall.getId(), dto.id());
        assertEquals("Halle A", dto.name());
    }

    @Test
    void getById_with_includeInactive_requires_cookie_and_uses_manage_use_case() {
        Hall hall = hall(false, "Halle B", HallType.FULL);
        User admin = admin();

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> resource.getById(hall.getId(), true, null)
        );
        assertEquals("Missing session cookie", ex.getMessage());

        when(getCurrentUserUseCase.getCurrentUser("sess")).thenReturn(admin);
        when(manageHallUseCase.getByIdIncludingInactive(admin.getId(), hall.getId())).thenReturn(hall);

        HallDTO dto = resource.getById(hall.getId(), true, "sess");
        assertEquals(hall.getId(), dto.id());
        assertEquals(false, dto.active());
    }
}

