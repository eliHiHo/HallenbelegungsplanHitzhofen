package de.hallenbelegung.adapters.in.api.dto;

import java.util.UUID;

public record UserDTO(
        UUID id,
        String username,
        String fullName,
        String email,
        String role,
        boolean active
) {
}
