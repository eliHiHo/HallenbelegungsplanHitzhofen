package de.hallenbelegung.application.domain.port.in;

public interface CancelBookingUseCase {

    void cancel(Long bookingId, Long userId);
}