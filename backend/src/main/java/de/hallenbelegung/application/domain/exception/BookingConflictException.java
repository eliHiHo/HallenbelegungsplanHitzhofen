package de.hallenbelegung.application.domain.exception;

public class BookingConflictException extends DomainException {

    public BookingConflictException(String message) {
        super("BOOKING_CONFLICT", message);
    }
}
