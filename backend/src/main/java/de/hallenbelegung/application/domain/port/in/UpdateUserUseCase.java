package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.model.Role;

import java.util.UUID;

public interface UpdateUserUseCase {
    void updateUser(UUID adminUserId,
                    UUID userId,
                    String firstName,
                    String lastName,
                    String email,
                    Role role,
                    Boolean active);
}