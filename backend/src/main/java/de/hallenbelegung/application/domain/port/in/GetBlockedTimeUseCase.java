package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.model.BlockedTime;

import java.util.List;

public interface GetBlockedTimeUseCase {
    List<BlockedTime> getAll(Long adminUserId);
    BlockedTime getById(Long adminUserId, Long blockedTimeId);
}