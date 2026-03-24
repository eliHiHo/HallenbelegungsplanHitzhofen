package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.model.Role;

public interface CreateUserUseCase {
    Long createUser(Long adminUserId,
                    String firstName,
                    String lastName,
                    String email,
                    String rawPassword,
                    Role role);
}