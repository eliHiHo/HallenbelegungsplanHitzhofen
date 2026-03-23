package de.hallenbelegung.application.domain.port.out;

import de.hallenbelegung.application.domain.model.BlockedTime;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BlockedTimeRepositoryPort {

    BlockedTime save(BlockedTime blockedTime);

    Optional<BlockedTime> findById(Long blockedTimeId);

    List<BlockedTime> findByHallIdAndTimeRange(Long hallId, LocalDateTime startTime, LocalDateTime endTime);

    List<BlockedTime> findAllByTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    void deleteById(Long blockedTimeId);
}