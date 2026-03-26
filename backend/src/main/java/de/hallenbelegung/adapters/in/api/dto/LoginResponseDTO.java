package de.hallenbelegung.adapters.in.api.dto;

import java.util.UUID;

public record LoginResponseDTO(
        String token,
        UUID userId,
        String username,
        String role
) {
}
