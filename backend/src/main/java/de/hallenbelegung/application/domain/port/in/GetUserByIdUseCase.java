package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.model.User;

public interface GetUserByIdUseCase {
    User getUserById(Long adminUserId, Long userId);
}