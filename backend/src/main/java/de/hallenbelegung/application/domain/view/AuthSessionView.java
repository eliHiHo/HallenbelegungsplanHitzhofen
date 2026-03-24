package de.hallenbelegung.application.domain.view;

import de.hallenbelegung.application.domain.model.Role;

import java.util.UUID;

public class AuthSessionView {

    private final String sessionId;
    private final UUID userId;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final Role role;

    public AuthSessionView(String sessionId,
                           UUID userId,
                           String firstName,
                           String lastName,
                           String email,
                           Role role) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
    }

    public String getSessionId() {
        return sessionId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }
}