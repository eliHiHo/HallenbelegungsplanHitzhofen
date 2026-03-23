package de.hallenbelegung.application.domain.port.in;

import java.time.LocalDateTime;

public interface CreateBlockedTimeUseCase {

    void create(Long hallId, String reason,
                LocalDateTime startTime, LocalDateTime endTime,
                Long adminUserId);
}
