package de.hallenbelegung.application.domain.port.out;

import de.hallenbelegung.application.domain.model.BookingSeries;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookingSeriesRepositoryPort {

    BookingSeries save(BookingSeries bookingSeries);

    Optional<BookingSeries> findById(Long bookingSeriesId);

    List<BookingSeries> findByHallId(Long hallId);

    List<BookingSeries> findActiveByHallIdAndDateRange(Long hallId, LocalDate startDate, LocalDate endDate);

    List<BookingSeries> findByResponsibleUserId(Long userId);

    List<BookingSeries> findAll();
}