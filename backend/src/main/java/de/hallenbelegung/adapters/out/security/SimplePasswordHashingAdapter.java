package de.hallenbelegung.adapters.out.security;

import de.hallenbelegung.application.domain.port.out.PasswordHashingPort;
import jakarta.enterprise.context.ApplicationScoped;

import org.mindrot.jbcrypt.BCrypt;

@ApplicationScoped
public class SimplePasswordHashingAdapter implements PasswordHashingPort {

    private static final int LOG_ROUNDS = 10; // reasonable default

    @Override
    public String hash(String rawPassword) {
        if (rawPassword == null) return null;
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(LOG_ROUNDS));
    }
}
