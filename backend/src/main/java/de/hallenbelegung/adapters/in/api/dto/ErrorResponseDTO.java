package de.hallenbelegung.adapters.in.api.dto;

import java.time.Instant;

public record ErrorResponseDTO(
        int status,
        String error,
        String message,
        Instant timestamp,
        String path
) {
}
