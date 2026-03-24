package de.hallenbelegung.application.domain.port.in;

public interface ResetPasswordUseCase {
    void resetPassword(String token, String newPassword);
}