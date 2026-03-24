package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.model.Role;

import java.util.UUID;

public interface CreateUserUseCase {
    UUID createUser(UUID adminUserId,
                    String firstName,
                    String lastName,
                    String email,
                    String rawPassword,
                    Role role);
}