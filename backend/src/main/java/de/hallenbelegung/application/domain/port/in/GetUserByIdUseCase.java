package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.model.User;

import java.util.UUID;

public interface GetUserByIdUseCase {
    User getUserById(UUID adminUserId, UUID userId);
}