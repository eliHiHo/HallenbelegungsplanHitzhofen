package de.hallenbelegung.application.domain.port.in;

public interface RequestPasswordResetUseCase {
    void requestPasswordReset(String email);
}
