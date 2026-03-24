package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.model.User;

public interface GetCurrentUserUseCase {
    User getCurrentUser(String sessionId);
}