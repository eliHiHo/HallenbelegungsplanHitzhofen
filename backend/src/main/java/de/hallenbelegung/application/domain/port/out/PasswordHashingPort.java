package de.hallenbelegung.application.domain.port.out;

public interface PasswordHashingPort {
    String hash(String rawPassword);
}