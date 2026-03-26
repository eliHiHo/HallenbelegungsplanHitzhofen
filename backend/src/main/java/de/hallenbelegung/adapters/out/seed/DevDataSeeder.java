package de.hallenbelegung.adapters.out.seed;

import de.hallenbelegung.application.domain.model.Hall;
import de.hallenbelegung.application.domain.model.HallType;
import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.out.HallRepositoryPort;
import de.hallenbelegung.application.domain.port.out.PasswordHashingPort;
import de.hallenbelegung.application.domain.port.out.UserRepositoryPort;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.arc.profile.IfBuildProfile;

@ApplicationScoped
@IfBuildProfile("dev")
public class DevDataSeeder {

    @Inject
    UserRepositoryPort userRepository;

    @Inject
    HallRepositoryPort hallRepository;

    @Inject
    PasswordHashingPort passwordHashingPort;

    @PostConstruct
    void init() {
        try {
            if (userRepository.findAllActive().isEmpty()) {
                // create admin
                String adminHash = passwordHashingPort.hash("adminpass");
                User admin = User.createNew("System","Admin","admin@example.com", adminHash, Role.ADMIN);
                userRepository.save(admin);

                // optional club representative
                String clubHash = passwordHashingPort.hash("clubpass");
                User club = User.createNew("Club","Rep","club@example.com", clubHash, Role.CLUB_REPRESENTATIVE);
                userRepository.save(club);
            }

            if (hallRepository.findAllActive().isEmpty()) {
                Hall hallA = Hall.createNew("Halle A","Kleine Halle", HallType.PART_SMALL);
                hallRepository.save(hallA);
                Hall hallB = Hall.createNew("Halle B","Grosse Halle", HallType.PART_LARGE);
                hallRepository.save(hallB);
                Hall full = Hall.createNew("Gesamthalle","Vollstaendige Halle", HallType.FULL);
                hallRepository.save(full);
            }
        } catch (Exception e) {
            // don't fail startup if seeding fails
            e.printStackTrace();
        }
    }
}
