package de.hallenbelegung.adapters.out.mail;

import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.out.PasswordResetMailPort;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class NoopPasswordResetMailAdapter implements PasswordResetMailPort {
    @Override
    public void sendPasswordResetMail(User user, String resetTokenOrLink) {
        // Intentionally no-op for now. Infrastructure can provide real mail sender.
    }
}
