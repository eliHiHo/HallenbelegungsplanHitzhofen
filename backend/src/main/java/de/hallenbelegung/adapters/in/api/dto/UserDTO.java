package de.hallenbelegung.adapters.in.api.dto;

import java.util.UUID;

public record UserDTO(
        UUID id,
        String firstName,
        String lastName,
        String fullName,
        String email,
        String role,
        boolean active
) {
}
