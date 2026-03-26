package de.hallenbelegung.adapters.in.api.mapper;

import de.hallenbelegung.adapters.in.api.dto.LoginResponseDTO;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.view.AuthSessionView;

public class AuthApiMapper {

    public static LoginResponseDTO toLoginResponse(AuthSessionView view) {
        if (view == null) {
            return null;
        }

        return new LoginResponseDTO(
                view.getSessionId(),
                view.getUserId(),
                view.getEmail(),
                view.getFirstName(),
                view.getLastName(),
                view.getRole().name()
        );
    }
}
