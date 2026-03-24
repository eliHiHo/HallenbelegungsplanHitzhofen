package de.hallenbelegung.application.domain.port.in;

public interface LogoutUseCase {
    void logout(String sessionId);
}