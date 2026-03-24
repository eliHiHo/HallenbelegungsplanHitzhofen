package de.hallenbelegung.application.domain.port.in;

public interface DeleteBlockedTimeUseCase {
    void delete(Long blockedTimeId, Long adminUserId);
}
