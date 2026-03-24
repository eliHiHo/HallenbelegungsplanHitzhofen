package de.hallenbelegung.application.domain.view;

public class SessionUserView {

    private final String sessionId;
    private final Long userId;

    public SessionUserView(String sessionId, Long userId) {
        this.sessionId = sessionId;
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Long getUserId() {
        return userId;
    }
}