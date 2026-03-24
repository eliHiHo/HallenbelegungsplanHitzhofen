package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.model.User;

import java.util.List;

public interface GetUsersUseCase {
    List<User> getAllUsers(Long adminUserId);
}