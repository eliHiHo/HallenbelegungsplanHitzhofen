package de.hallenbelegung.application.domain.port.out;

import de.hallenbelegung.application.domain.model.BlockedTime;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BlockedTimeRepositoryPort {

    BlockedTime save(BlockedTime blockedTime);

    Optional<BlockedTime> findById(UUID blockedTimeId);

    List<BlockedTime> findByHallIdAndTimeRange(UUID hallId, LocalDateTime startTime, LocalDateTime endTime);

    List<BlockedTime> findAllByTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    void deleteById(UUID blockedTimeId);

    List<BlockedTime> findAll();
}