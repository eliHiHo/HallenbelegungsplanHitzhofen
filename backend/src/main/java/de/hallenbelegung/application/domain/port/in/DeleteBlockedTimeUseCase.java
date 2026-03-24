package de.hallenbelegung.application.domain.port.in;

import java.util.UUID;

public interface DeleteBlockedTimeUseCase {
    void delete(UUID blockedTimeId, UUID adminUserId);
}
