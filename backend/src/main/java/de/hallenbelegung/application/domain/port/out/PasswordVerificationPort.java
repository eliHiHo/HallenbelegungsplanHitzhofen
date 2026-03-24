package de.hallenbelegung.application.domain.port.out;

public interface PasswordVerificationPort {
    boolean matches(String rawPassword, String passwordHash);
}