package de.hallenbelegung.application.domain.port.out;

import de.hallenbelegung.application.domain.model.BookingSeries;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingSeriesRepositoryPort {

    BookingSeries save(BookingSeries bookingSeries);

    Optional<BookingSeries> findById(UUID bookingSeriesId);

    List<BookingSeries> findByHallId(UUID hallId);

    List<BookingSeries> findActiveByHallIdAndDateRange(UUID hallId, LocalDate startDate, LocalDate endDate);

    List<BookingSeries> findByResponsibleUserId(UUID userId);

    List<BookingSeries> findAll();
}