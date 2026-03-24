package de.hallenbelegung.application.domain.port.out;

import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.view.SessionUserView;

import java.time.Duration;
import java.util.Optional;

public interface SessionPort {

    String createSession(User user, Duration inactivityTimeout);

    Optional<SessionUserView> findActiveSession(String sessionId);

    void invalidateSession(String sessionId);

    void invalidateSessionsByUserId(Long userId);

    void touchSession(String sessionId, Duration inactivityTimeout);
}