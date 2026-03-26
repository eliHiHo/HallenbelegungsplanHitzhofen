package de.hallenbelegung.adapters.in.api.mapper;

import de.hallenbelegung.adapters.in.api.dto.LoginResponseDTO;
import de.hallenbelegung.application.domain.model.User;

public class AuthApiMapper {

    public static LoginResponseDTO toLoginResponse(String token, User user) {
        if (user == null) {
            return new LoginResponseDTO(token, null, null, null, null, null);
        }

        return new LoginResponseDTO(
                token,
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole() != null ? user.getRole().name() : null
        );
    }
}
