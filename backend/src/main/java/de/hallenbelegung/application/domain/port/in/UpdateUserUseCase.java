package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.model.Role;

public interface UpdateUserUseCase {
    void updateUser(Long adminUserId,
                    Long userId,
                    String firstName,
                    String lastName,
                    String email,
                    Role role,
                    Boolean active);
}