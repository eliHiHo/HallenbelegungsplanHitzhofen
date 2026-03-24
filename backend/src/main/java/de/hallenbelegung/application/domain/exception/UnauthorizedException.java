package de.hallenbelegung.application.domain.exception;

public class UnauthorizedException extends DomainException {

    public UnauthorizedException(String message) {
        super("UNAUTHORIZED", message);
    }
}