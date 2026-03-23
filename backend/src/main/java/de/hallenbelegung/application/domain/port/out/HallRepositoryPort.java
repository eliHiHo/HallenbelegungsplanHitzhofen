package de.hallenbelegung.application.domain.port.out;

import de.hallenbelegung.application.domain.model.Hall;

import java.util.List;
import java.util.Optional;

public interface HallRepositoryPort {

    Hall save(Hall hall);

    Optional<Hall> findById(Long hallId);

    List<Hall> findAll();

    List<Hall> findAllActive();
}