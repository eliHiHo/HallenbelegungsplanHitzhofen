package de.hallenbelegung.application.domain.service;

import de.hallenbelegung.application.domain.exception.ForbiddenException;
import de.hallenbelegung.application.domain.exception.NotFoundException;
import de.hallenbelegung.application.domain.model.Hall;
import de.hallenbelegung.application.domain.model.HallType;
import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.out.HallRepositoryPort;
import de.hallenbelegung.application.domain.port.out.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class HallServiceTest {

    private HallRepositoryPort hallRepository;
    private UserRepositoryPort userRepository;

    private HallService service;

    @BeforeEach
    void setUp() {
        hallRepository = mock(HallRepositoryPort.class);
        userRepository = mock(UserRepositoryPort.class);

        service = new HallService(hallRepository, userRepository);
    }

    private User createAdmin() {
        return new User(
                UUID.randomUUID(),
                "Admin",
                "User",
                "admin@example.com",
                "hash",
                Role.ADMIN,
                true,
                Instant.now(),
                Instant.now()
        );
    }

    private User createRepresentative() {
        return new User(
                UUID.randomUUID(),
                "Club",
                "Rep",
                "rep@example.com",
                "hash",
                Role.CLUB_REPRESENTATIVE,
                true,
                Instant.now(),
                Instant.now()
        );
    }

    private User createInactiveAdmin() {
        return new User(
                UUID.randomUUID(),
                "Inactive",
                "Admin",
                "inactive-admin@example.com",
                "hash",
                Role.ADMIN,
                false,
                Instant.now(),
                Instant.now()
        );
    }

    private Hall createActiveHallA() {
        return new Hall(
                UUID.randomUUID(),
                "Halle A",
                "Teilhalle A",
                true,
                Instant.now(),
                Instant.now(),
                HallType.PART_SMALL
        );
    }

    private Hall createActiveHallB() {
        return new Hall(
                UUID.randomUUID(),
                "Halle B",
                "Teilhalle B",
                true,
                Instant.now(),
                Instant.now(),
                HallType.PART_LARGE
        );
    }

    private Hall createActiveFullHall() {
        return new Hall(
                UUID.randomUUID(),
                "Gesamthalle",
                "Die ganze Halle",
                true,
                Instant.now(),
                Instant.now(),
                HallType.FULL
        );
    }

    private Hall createInactiveHall() {
        return new Hall(
                UUID.randomUUID(),
                "Inaktive Halle",
                "Nicht sichtbar",
                false,
                Instant.now(),
                Instant.now(),
                HallType.PART_SMALL
        );
    }

    @Test
    void getAllActive_returns_only_active_halls() {
        Hall hallA = createActiveHallA();
        Hall hallB = createActiveHallB();
        Hall fullHall = createActiveFullHall();

        when(hallRepository.findAllActive()).thenReturn(List.of(hallA, hallB, fullHall));

        List<Hall> result = service.getAllActive();

        assertEquals(3, result.size());
        assertEquals(hallA.getId(), result.get(0).getId());
        assertEquals(hallB.getId(), result.get(1).getId());
        assertEquals(fullHall.getId(), result.get(2).getId());
    }

    @Test
    void getAllActive_returns_empty_list_when_no_active_halls_exist() {
        when(hallRepository.findAllActive()).thenReturn(List.of());

        List<Hall> result = service.getAllActive();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getById_returns_active_hall_without_authentication() {
        Hall hall = createActiveHallA();

        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        Hall result = service.getById(hall.getId());

        assertEquals(hall.getId(), result.getId());
        assertEquals("Halle A", result.getName());
        assertTrue(result.isActive());
    }

    @Test
    void getById_rejects_unknown_hall() {
        UUID hallId = UUID.randomUUID();

        when(hallRepository.findById(hallId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.getById(hallId)
        );

        assertEquals("Hall not found", exception.getMessage());
    }

    @Test
    void getById_rejects_inactive_hall_for_public_access() {
        Hall inactiveHall = createInactiveHall();

        when(hallRepository.findById(inactiveHall.getId())).thenReturn(Optional.of(inactiveHall));

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.getById(inactiveHall.getId())
        );

        assertEquals("Hall not found", exception.getMessage());
    }

    @Test
    void getAllIncludingInactive_allows_admin() {
        User admin = createAdmin();
        Hall hallA = createActiveHallA();
        Hall inactiveHall = createInactiveHall();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(hallRepository.findAll()).thenReturn(List.of(hallA, inactiveHall));

        List<Hall> result = service.getAllIncludingInactive(admin.getId());

        assertEquals(2, result.size());
        assertEquals(hallA.getId(), result.get(0).getId());
        assertEquals(inactiveHall.getId(), result.get(1).getId());
    }

    @Test
    void getAllIncludingInactive_rejects_unknown_user() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.getAllIncludingInactive(userId)
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void getAllIncludingInactive_rejects_non_admin() {
        User representative = createRepresentative();

        when(userRepository.findById(representative.getId())).thenReturn(Optional.of(representative));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.getAllIncludingInactive(representative.getId())
        );

        assertEquals("User not allowed to view inactive halls", exception.getMessage());
    }

    @Test
    void getAllIncludingInactive_rejects_inactive_admin() {
        User inactiveAdmin = createInactiveAdmin();

        when(userRepository.findById(inactiveAdmin.getId())).thenReturn(Optional.of(inactiveAdmin));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.getAllIncludingInactive(inactiveAdmin.getId())
        );

        assertEquals("User inactive", exception.getMessage());
    }

    @Test
    void getByIdIncludingInactive_allows_admin_for_active_hall() {
        User admin = createAdmin();
        Hall hall = createActiveHallA();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        Hall result = service.getByIdIncludingInactive(admin.getId(), hall.getId());

        assertEquals(hall.getId(), result.getId());
    }

    @Test
    void getByIdIncludingInactive_allows_admin_for_inactive_hall() {
        User admin = createAdmin();
        Hall inactiveHall = createInactiveHall();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(hallRepository.findById(inactiveHall.getId())).thenReturn(Optional.of(inactiveHall));

        Hall result = service.getByIdIncludingInactive(admin.getId(), inactiveHall.getId());

        assertEquals(inactiveHall.getId(), result.getId());
        assertFalse(result.isActive());
    }

    @Test
    void getByIdIncludingInactive_rejects_unknown_user() {
        UUID userId = UUID.randomUUID();
        UUID hallId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.getByIdIncludingInactive(userId, hallId)
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void getByIdIncludingInactive_rejects_non_admin() {
        User representative = createRepresentative();
        Hall hall = createActiveHallA();

        when(userRepository.findById(representative.getId())).thenReturn(Optional.of(representative));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.getByIdIncludingInactive(representative.getId(), hall.getId())
        );

        assertEquals("User not allowed to view inactive halls", exception.getMessage());
    }

    @Test
    void getByIdIncludingInactive_rejects_inactive_admin() {
        User inactiveAdmin = createInactiveAdmin();
        Hall hall = createActiveHallA();

        when(userRepository.findById(inactiveAdmin.getId())).thenReturn(Optional.of(inactiveAdmin));
        when(hallRepository.findById(hall.getId())).thenReturn(Optional.of(hall));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.getByIdIncludingInactive(inactiveAdmin.getId(), hall.getId())
        );

        assertEquals("User inactive", exception.getMessage());
    }

    @Test
    void getByIdIncludingInactive_rejects_unknown_hall() {
        User admin = createAdmin();
        UUID hallId = UUID.randomUUID();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(hallRepository.findById(hallId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.getByIdIncludingInactive(admin.getId(), hallId)
        );

        assertEquals("Hall not found", exception.getMessage());
    }
}