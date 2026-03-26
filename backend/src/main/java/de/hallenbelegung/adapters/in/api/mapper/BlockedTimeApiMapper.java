package de.hallenbelegung.adapters.in.api.mapper;

import de.hallenbelegung.adapters.in.api.dto.BlockedTimeDTO;
import de.hallenbelegung.application.domain.model.BlockedTime;

public class BlockedTimeApiMapper {

    public static BlockedTimeDTO toDTO(BlockedTime b) {
        return new BlockedTimeDTO(
                b.getId(),
                b.getReason(),
                b.getStartAt(),
                b.getEndAt(),
                b.getHall().getId(),
                b.getHall().getName()
        );
    }
}
