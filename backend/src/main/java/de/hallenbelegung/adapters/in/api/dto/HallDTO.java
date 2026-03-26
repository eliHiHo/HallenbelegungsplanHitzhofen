package de.hallenbelegung.adapters.in.api.dto;

import java.util.UUID;

public record HallDTO(
        UUID id,
        String name,
        String description,
        String type,
        boolean active
) {
}
