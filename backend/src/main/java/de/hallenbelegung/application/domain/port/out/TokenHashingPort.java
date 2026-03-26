package de.hallenbelegung.application.domain.port.out;

public interface TokenHashingPort {
    String hash(String raw);
}
