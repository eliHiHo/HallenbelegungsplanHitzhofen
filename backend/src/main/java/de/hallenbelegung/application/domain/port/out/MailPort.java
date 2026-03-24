package de.hallenbelegung.application.domain.port.out;

public interface MailPort {
    void sendPasswordResetMail(String email, String resetToken);
}