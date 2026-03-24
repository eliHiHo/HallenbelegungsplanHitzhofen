package de.hallenbelegung.application.domain.port.out;

import de.hallenbelegung.application.domain.model.User;

public interface PasswordResetMailPort {
    void sendPasswordResetMail(User user, String resetTokenOrLink);
}