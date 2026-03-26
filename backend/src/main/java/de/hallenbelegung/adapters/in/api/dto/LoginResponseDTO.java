package de.hallenbelegung.adapters.in.api.dto;

import java.util.UUID;

public record LoginResponseDTO(
        String token,
        UUID userId,
        String email,
        String firstName,
        String lastName,
        String role
) {
}
