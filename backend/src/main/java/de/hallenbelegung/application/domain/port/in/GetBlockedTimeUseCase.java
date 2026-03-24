package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.model.BlockedTime;

import java.util.List;
import java.util.UUID;

public interface GetBlockedTimeUseCase {
    List<BlockedTime> getAll(UUID adminUserId);
    BlockedTime getById(UUID adminUserId, UUID blockedTimeId);
}