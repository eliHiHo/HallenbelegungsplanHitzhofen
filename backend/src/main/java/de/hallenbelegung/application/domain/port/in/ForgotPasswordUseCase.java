package de.hallenbelegung.application.domain.port.in;

public interface ForgotPasswordUseCase {
    void requestPasswordReset(String email);
}