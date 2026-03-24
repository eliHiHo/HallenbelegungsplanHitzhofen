package de.hallenbelegung.application.domain.port.out;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetPort {
    String createToken(UUID userId, Duration validity);
    Optional<UUID> findUserIdByValidToken(String token);
    void invalidateToken(String token);
    void invalidateTokensByUserId(UUID userId);
}