package de.hallenbelegung.application.domain.port.out;

import de.hallenbelegung.application.domain.model.BookingRequestStatus;
import de.hallenbelegung.application.domain.model.BookingSeriesRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingSeriesRequestRepositoryPort {

    BookingSeriesRequest save(BookingSeriesRequest bookingSeriesRequest);

    Optional<BookingSeriesRequest> findById(UUID bookingSeriesRequestId);

    List<BookingSeriesRequest> findAll();

    List<BookingSeriesRequest> findByStatus(BookingRequestStatus status);

    List<BookingSeriesRequest> findByRequestedByUserId(UUID userId);
}