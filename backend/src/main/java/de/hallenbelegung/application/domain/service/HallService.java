package de.hallenbelegung.application.domain.service;

import de.hallenbelegung.application.domain.exception.ForbiddenException;
import de.hallenbelegung.application.domain.exception.NotFoundException;
import de.hallenbelegung.application.domain.model.Hall;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.in.GetHallUseCase;
import de.hallenbelegung.application.domain.port.in.ManageHallUseCase;
import de.hallenbelegung.application.domain.port.out.HallRepositoryPort;
import de.hallenbelegung.application.domain.port.out.UserRepositoryPort;
import jakarta.transaction.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class HallService implements GetHallUseCase, ManageHallUseCase {

    private final HallRepositoryPort hallRepository;
    private final UserRepositoryPort userRepository;

    public HallService(HallRepositoryPort hallRepository,
                       UserRepositoryPort userRepository) {
        this.hallRepository = hallRepository;
        this.userRepository = userRepository;
    }

    /**
     * Öffentliche/standardmäßige Hallenliste:
     * nur aktive Hallen, sortiert für stabile Anzeige im Frontend.
     */
    public List<Hall> getAllActive() {
        return hallRepository.findAllActive()
                .stream()
                .sorted(Comparator.comparing(Hall::getId))
                .toList();
    }

    /**
     * Liefert eine einzelne Halle, sofern sie aktiv ist.
     * Für öffentliche Nutzung bzw. normale Standardabfragen gedacht.
     */
    public Hall getById(UUID hallId) {
        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new NotFoundException("Hall not found"));

        if (!hall.isActive()) {
            throw new NotFoundException("Hall not found");
        }

        return hall;
    }

    /**
     * Admin-Funktion:
     * liefert alle Hallen inklusive inaktiver Hallen.
     */
    public List<Hall> getAllIncludingInactive(UUID currentUserId) {
        User user = loadActiveUser(currentUserId);

        if (!user.isAdmin()) {
            throw new ForbiddenException("User not allowed to view all halls");
        }

        return hallRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Hall::getId))
                .toList();
    }

    /**
     * Admin-Funktion:
     * lädt eine Halle unabhängig vom Aktivstatus.
     */
    public Hall getByIdIncludingInactive(UUID currentUserId, UUID hallId) {
        User user = loadActiveUser(currentUserId);

        if (!user.isAdmin()) {
            throw new ForbiddenException("User not allowed to view this hall");
        }

        return hallRepository.findById(hallId)
                .orElseThrow(() -> new NotFoundException("Hall not found"));
    }

    private User loadActiveUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!user.isActive()) {
            throw new ForbiddenException("User inactive");
        }

        return user;
    }
}