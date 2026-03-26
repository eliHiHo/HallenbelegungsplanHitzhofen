package de.hallenbelegung.application.domain.exception;

public class RateLimitException extends DomainException {

    public RateLimitException(String message) {
        super("RATE_LIMIT_EXCEEDED", message);
    }
}
