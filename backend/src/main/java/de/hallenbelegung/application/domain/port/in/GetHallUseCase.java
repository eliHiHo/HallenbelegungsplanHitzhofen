package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.model.Hall;

import java.util.List;
import java.util.UUID;

public interface GetHallUseCase {
    List<Hall> getAllActive();
    Hall getById(UUID hallId);
}