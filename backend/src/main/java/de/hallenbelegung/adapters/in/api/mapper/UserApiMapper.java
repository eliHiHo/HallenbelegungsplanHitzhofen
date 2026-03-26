package de.hallenbelegung.adapters.in.api.mapper;

import de.hallenbelegung.adapters.in.api.dto.UserDTO;
import de.hallenbelegung.application.domain.model.User;

public class UserApiMapper {

    public static UserDTO toDTO(User u) {
        return new UserDTO(
                u.getId(),
                u.getFirstName(),
                u.getLastName(),
                u.getFullName(),
                u.getEmail(),
                u.getRole().name(),
                u.isActive()
        );
    }
}
