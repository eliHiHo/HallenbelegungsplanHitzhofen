package de.hallenbelegung.adapters.out.security;

import de.hallenbelegung.application.domain.port.out.PasswordVerificationPort;
import jakarta.enterprise.context.ApplicationScoped;

import org.mindrot.jbcrypt.BCrypt;

@ApplicationScoped
public class SimplePasswordVerificationAdapter implements PasswordVerificationPort {

    @Override
    public boolean matches(String rawPassword, String hash) {
        if (rawPassword == null || hash == null) return false;
        try {
            return BCrypt.checkpw(rawPassword, hash);
        } catch (Exception e) {
            return false;
        }
    }
}
