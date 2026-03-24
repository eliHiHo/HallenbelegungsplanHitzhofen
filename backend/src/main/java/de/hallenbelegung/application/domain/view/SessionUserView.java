package de.hallenbelegung.application.domain.view;

import java.util.UUID;

public class SessionUserView {

    private final String sessionId;
    private final UUID userId;

    public SessionUserView(String sessionId, UUID userId) {
        this.sessionId = sessionId;
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public UUID getUserId() {
        return userId;
    }
}