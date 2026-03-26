package de.hallenbelegung.adapters.in.api.mapper;

import de.hallenbelegung.adapters.in.api.dto.HallDTO;
import de.hallenbelegung.application.domain.model.Hall;

public class HallApiMapper {

    public static HallDTO toDTO(Hall h) {
        return new HallDTO(
                h.getId(),
                h.getName(),
                h.getDescription(),
                h.getHallType().name(),
                h.isActive()
        );
    }
}
