package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.model.User;

import java.util.List;
import java.util.UUID;

public interface GetUsersUseCase {
    List<User> getAllUsers(UUID adminUserId);
}