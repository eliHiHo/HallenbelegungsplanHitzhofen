package de.hallenbelegung.application.domain.port.out;

import de.hallenbelegung.application.domain.model.Hall;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HallRepositoryPort {

    Hall save(Hall hall);

    Optional<Hall> findById(UUID hallId);

    List<Hall> findAll();

    List<Hall> findAllActive();
}