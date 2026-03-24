package de.hallenbelegung.application.domain.port.out;

import java.time.Duration;
import java.util.Optional;

public interface PasswordResetPort {

    String createToken(Long userId, Duration validity);

    Optional<Long> findUserIdByValidToken(String token);

    void invalidateToken(String token);

    void invalidateTokensByUserId(Long userId);
}