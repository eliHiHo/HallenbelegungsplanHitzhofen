package de.hallenbelegung.adapters.in.api.mapper;

import de.hallenbelegung.adapters.in.api.dto.LoginResponseDTO;
import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.view.AuthSessionView;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuthApiMapperTest {

    @Test
    void toLoginResponse_maps_view_and_handles_null() {
        assertNull(AuthApiMapper.toLoginResponse(null));

        UUID userId = UUID.randomUUID();
        AuthSessionView view = new AuthSessionView("sess", userId, "Max", "Mustermann", "max@example.com", Role.ADMIN);

        LoginResponseDTO dto = AuthApiMapper.toLoginResponse(view);

        assertEquals("sess", dto.token());
        assertEquals(userId, dto.userId());
        assertEquals("ADMIN", dto.role());
    }
}

