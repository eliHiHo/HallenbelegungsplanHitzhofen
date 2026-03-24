package de.hallenbelegung.application.domain.port.out;

import de.hallenbelegung.application.domain.model.Booking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepositoryPort {

    Booking save(Booking booking);

    Optional<Booking> findById(UUID bookingId);

    List<Booking> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    List<Booking> findByHallIdAndTimeRange(UUID hallId, LocalDateTime startTime, LocalDateTime endTime);

    List<Booking> findByResponsibleUserId(UUID userId);

    List<Booking> findByBookingSeriesId(UUID bookingSeriesId);
}