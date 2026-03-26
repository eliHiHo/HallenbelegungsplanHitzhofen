package de.hallenbelegung.adapters.in.api.mapper;

import de.hallenbelegung.adapters.in.api.dto.LoginResponseDTO;
import de.hallenbelegung.application.domain.model.User;

public class AuthApiMapper {

    public static LoginResponseDTO toLoginResponse(String token, User user) {
        return new LoginResponseDTO(
                token,
                user != null ? user.getId() : null,
                user != null ? user.getEmail() : null,
                user != null ? user.getRole().name() : null
        );
    }
}
