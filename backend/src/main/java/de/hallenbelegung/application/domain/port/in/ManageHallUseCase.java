package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.model.Hall;

import java.util.List;
import java.util.UUID;

public interface ManageHallUseCase {
    List<Hall> getAllIncludingInactive(UUID currentUserId);
    Hall getByIdIncludingInactive(UUID currentUserId, UUID hallId);
}