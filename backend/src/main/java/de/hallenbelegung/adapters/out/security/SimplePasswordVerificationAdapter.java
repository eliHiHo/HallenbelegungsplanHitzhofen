package de.hallenbelegung.adapters.out.security;

import de.hallenbelegung.application.domain.port.out.PasswordVerificationPort;
import de.hallenbelegung.application.domain.port.out.PasswordHashingPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SimplePasswordVerificationAdapter implements PasswordVerificationPort {

    @Inject
    PasswordHashingPort hashing;

    @Override
    public boolean matches(String rawPassword, String hash) {
        if (rawPassword == null || hash == null) return false;
        return hashing.hash(rawPassword).equals(hash);
    }
}
