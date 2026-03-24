package de.hallenbelegung.application.domain.exception;

public class ValidationException extends DomainException {

    public ValidationException(String message) {
        super("VALIDATION_ERROR", message);
    }
}