package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.model.Hall;

import java.util.List;

public interface ManageHallUseCase {
    List<Hall> getAllIncludingInactive(Long currentUserId);
    Hall getByIdIncludingInactive(Long currentUserId, Long hallId);
}