package de.hallenbelegung.application.domain.port.out;

import de.hallenbelegung.application.domain.model.Booking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepositoryPort {

    Booking save(Booking booking);

    Optional<Booking> findById(Long bookingId);

    List<Booking> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    List<Booking> findByHallIdAndTimeRange(Long hallId, LocalDateTime startTime, LocalDateTime endTime);

    List<Booking> findByResponsibleUserId(Long userId);

    List<Booking> findByBookingSeriesId(Long bookingSeriesId);
}