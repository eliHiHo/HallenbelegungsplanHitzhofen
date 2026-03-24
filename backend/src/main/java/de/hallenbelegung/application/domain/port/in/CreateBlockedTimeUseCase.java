package de.hallenbelegung.application.domain.port.in;

import java.time.LocalDateTime;
import java.util.UUID;

public interface CreateBlockedTimeUseCase {

    void create(UUID hallId, String reason,
                LocalDateTime startTime, LocalDateTime endTime,
                UUID adminUserId);
}
