package de.hallenbelegung.application.domain.port.out;

import de.hallenbelegung.application.domain.model.BookingRequest;
import de.hallenbelegung.application.domain.model.BookingRequestStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRequestRepositoryPort {

    BookingRequest save(BookingRequest bookingRequest);

    Optional<BookingRequest> findById(UUID bookingRequestId);

    List<BookingRequest> findAll();

    List<BookingRequest> findByStatus(BookingRequestStatus status);

    List<BookingRequest> findByRequestedByUserId(UUID userId);
}