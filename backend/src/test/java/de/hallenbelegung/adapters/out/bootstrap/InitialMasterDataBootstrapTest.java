package de.hallenbelegung.adapters.out.bootstrap;

import de.hallenbelegung.application.domain.model.Hall;
import de.hallenbelegung.application.domain.model.HallType;
import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.out.HallRepositoryPort;
import de.hallenbelegung.application.domain.port.out.PasswordHashingPort;
import de.hallenbelegung.application.domain.port.out.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InitialMasterDataBootstrapTest {

    private HallRepositoryPort hallRepository;
    private UserRepositoryPort userRepository;
    private PasswordHashingPort passwordHashingPort;

    private InitialMasterDataBootstrap bootstrap;

    @BeforeEach
    void setUp() {
        hallRepository = mock(HallRepositoryPort.class);
        userRepository = mock(UserRepositoryPort.class);
        passwordHashingPort = mock(PasswordHashingPort.class);

        bootstrap = new InitialMasterDataBootstrap();
        bootstrap.hallRepository = hallRepository;
        bootstrap.userRepository = userRepository;
        bootstrap.passwordHashingPort = passwordHashingPort;
        bootstrap.bootstrapEnabled = true;
        bootstrap.initialAdminEmail = Optional.of("admin@example.com");
        bootstrap.initialAdminPassword = Optional.of("secret");
        bootstrap.initialAdminFirstName = "System";
        bootstrap.initialAdminLastName = "Admin";
    }

    private Hall hall(String name, HallType type) {
        return new Hall(UUID.randomUUID(), name, "desc", true, Instant.now(), Instant.now(), type);
    }

    private User user(Role role, boolean active, String email) {
        return new User(UUID.randomUUID(), "A", "B", email, "hash", role, active, Instant.now(), Instant.now());
    }

    @Test
    void onStart_does_nothing_when_disabled() {
        bootstrap.bootstrapEnabled = false;

        bootstrap.onStart(null);

        verify(hallRepository, never()).findAllActive();
        verify(userRepository, never()).findAllActive();
    }

    @Test
    void onStart_creates_missing_halls_and_initial_admin() {
        when(hallRepository.findAllActive()).thenReturn(List.of(hall("Halle A", HallType.PART_SMALL)));
        when(userRepository.findAllActive()).thenReturn(List.of());
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(passwordHashingPort.hash("secret")).thenReturn("hashed-secret");

        bootstrap.onStart(null);

        verify(hallRepository, org.mockito.Mockito.times(2)).save(any(Hall.class));
        verify(passwordHashingPort).hash("secret");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void onStart_throws_when_no_admin_and_missing_email_config() {
        bootstrap.initialAdminEmail = Optional.empty();
        when(hallRepository.findAllActive()).thenReturn(List.of(
                hall("Halle A", HallType.PART_SMALL),
                hall("Halle B", HallType.PART_LARGE),
                hall("Gesamthalle", HallType.FULL)
        ));
        when(userRepository.findAllActive()).thenReturn(List.of());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> bootstrap.onStart(null));
        assertEquals("Missing required config: app.bootstrap.initial-admin.email", ex.getMessage());
    }

    @Test
    void onStart_throws_when_email_exists_but_no_active_admin() {
        when(hallRepository.findAllActive()).thenReturn(List.of(
                hall("Halle A", HallType.PART_SMALL),
                hall("Halle B", HallType.PART_LARGE),
                hall("Gesamthalle", HallType.FULL)
        ));
        when(userRepository.findAllActive()).thenReturn(List.of(user(Role.CLUB_REPRESENTATIVE, true, "rep@example.com")));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user(Role.CLUB_REPRESENTATIVE, true, "admin@example.com")));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> bootstrap.onStart(null));
        assertEquals(
                "Cannot create initial admin. User with email already exists but no active admin is available: admin@example.com",
                ex.getMessage()
        );
    }

    @Test
    void onStart_skips_admin_creation_when_active_admin_exists() {
        when(hallRepository.findAllActive()).thenReturn(List.of(
                hall("Halle A", HallType.PART_SMALL),
                hall("Halle B", HallType.PART_LARGE),
                hall("Gesamthalle", HallType.FULL)
        ));
        when(userRepository.findAllActive()).thenReturn(List.of(user(Role.ADMIN, true, "admin@example.com")));

        bootstrap.onStart(null);

        verify(userRepository, never()).save(any(User.class));
    }
}

