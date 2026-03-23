package de.hallenbelegung.application.domain.port.out;
import de.hallenbelegung.application.domain.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findById(Long userId);

    Optional<User> findByEmail(String email);

    List<User> findAll();

    List<User> findAllActive();
}
