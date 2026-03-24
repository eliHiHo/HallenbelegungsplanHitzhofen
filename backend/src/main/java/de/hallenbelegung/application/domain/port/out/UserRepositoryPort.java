package de.hallenbelegung.application.domain.port.out;
import de.hallenbelegung.application.domain.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findById(UUID userId);

    Optional<User> findByEmail(String email);

    List<User> findAll();

    List<User> findAllActive();
}
