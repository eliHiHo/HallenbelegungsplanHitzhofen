package de.hallenbelegung.adapters.out.bootstrap;

import de.hallenbelegung.application.domain.model.Hall;
import de.hallenbelegung.application.domain.model.HallType;
import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.out.HallRepositoryPort;
import de.hallenbelegung.application.domain.port.out.PasswordHashingPort;
import de.hallenbelegung.application.domain.port.out.UserRepositoryPort;
import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
@IfBuildProfile("prod")
public class InitialMasterDataBootstrap {

    @Inject
    HallRepositoryPort hallRepository;

    @Inject
    UserRepositoryPort userRepository;

    @Inject
    PasswordHashingPort passwordHashingPort;

    @ConfigProperty(name = "app.bootstrap.enabled", defaultValue = "true")
    boolean bootstrapEnabled;

    @ConfigProperty(name = "app.bootstrap.initial-admin.email")
    Optional<String> initialAdminEmail;

    @ConfigProperty(name = "app.bootstrap.initial-admin.password")
    Optional<String> initialAdminPassword;

    @ConfigProperty(name = "app.bootstrap.initial-admin.first-name", defaultValue = "System")
    String initialAdminFirstName;

    @ConfigProperty(name = "app.bootstrap.initial-admin.last-name", defaultValue = "Admin")
    String initialAdminLastName;

    @Transactional
    void onStart(@Observes StartupEvent event) {
        if (!bootstrapEnabled) {
            return;
        }

        ensureRequiredHalls();
        ensureInitialAdmin();
    }

    private void ensureRequiredHalls() {
        List<Hall> activeHalls = hallRepository.findAllActive();

        if (!hasHall(activeHalls, "Halle A")) {
            hallRepository.save(Hall.createNew("Halle A", "Kleine Halle", HallType.PART_SMALL));
        }

        if (!hasHall(activeHalls, "Halle B")) {
            hallRepository.save(Hall.createNew("Halle B", "Grosse Halle", HallType.PART_LARGE));
        }

        if (!hasHall(activeHalls, "Gesamthalle")) {
            hallRepository.save(Hall.createNew("Gesamthalle", "Vollstaendige Halle", HallType.FULL));
        }
    }

    private void ensureInitialAdmin() {
        boolean adminExists = userRepository.findAllActive()
                .stream()
                .anyMatch(User::isAdmin);

        if (adminExists) {
            return;
        }

        String email = initialAdminEmail
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new IllegalStateException(
                        "Missing required config: app.bootstrap.initial-admin.email"
                ));

        String rawPassword = initialAdminPassword
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new IllegalStateException(
                        "Missing required config: app.bootstrap.initial-admin.password"
                ));

        userRepository.findByEmail(email).ifPresent(existing -> {
            throw new IllegalStateException(
                    "Cannot create initial admin. User with email already exists but no active admin is available: " + email
            );
        });

        String passwordHash = passwordHashingPort.hash(rawPassword);

        User admin = User.createNew(
                initialAdminFirstName,
                initialAdminLastName,
                email,
                passwordHash,
                Role.ADMIN
        );

        userRepository.save(admin);
    }

    private boolean hasHall(List<Hall> halls, String name) {
        return halls.stream().anyMatch(hall -> hall.getName().equals(name));
    }
}