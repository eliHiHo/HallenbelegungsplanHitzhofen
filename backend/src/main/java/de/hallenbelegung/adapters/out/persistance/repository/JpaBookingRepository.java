package de.hallenbelegung.adapters.out.persistance.repository;

import de.hallenbelegung.adapters.out.persistance.entity.DBBooking;
import de.hallenbelegung.adapters.out.persistance.mapper.BookingPersistenceMapper;
import de.hallenbelegung.adapters.out.persistance.mapper.HallPersistenceMapper;
import de.hallenbelegung.adapters.out.persistance.mapper.UserPersistenceMapper;
import de.hallenbelegung.adapters.out.persistance.mapper.BookingSeriesPersistenceMapper;
import de.hallenbelegung.application.domain.model.Booking;
import de.hallenbelegung.application.domain.port.out.BookingRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class JpaBookingRepository implements BookingRepositoryPort {

    @Inject
    EntityManager em;

    private final BookingPersistenceMapper mapper = new BookingPersistenceMapper(
            new HallPersistenceMapper(),
            new UserPersistenceMapper(),
            new BookingSeriesPersistenceMapper(new HallPersistenceMapper(), new UserPersistenceMapper())
    );

    public JpaBookingRepository() {
    }

    @Override
    public Booking save(Booking booking) {
        DBBooking entity = mapper.toEntity(booking);
        DBBooking merged = em.merge(entity);
        return mapper.toDomain(merged);
    }

    @Override
    public Optional<Booking> findById(UUID bookingId) {
        DBBooking entity = em.find(DBBooking.class, bookingId);
        return Optional.ofNullable(mapper.toDomain(entity));
    }

    @Override
    public List<Booking> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        List<DBBooking> list = em.createQuery("SELECT b FROM DBBooking b WHERE b.startAt >= :start AND b.endAt <= :end", DBBooking.class)
                .setParameter("start", startTime)
                .setParameter("end", endTime)
                .getResultList();
        return list.stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Booking> findByHallIdAndTimeRange(UUID hallId, LocalDateTime startTime, LocalDateTime endTime) {
        List<DBBooking> list = em.createQuery("SELECT b FROM DBBooking b WHERE b.hall.id = :hallId AND b.startAt >= :start AND b.endAt <= :end", DBBooking.class)
                .setParameter("hallId", hallId)
                .setParameter("start", startTime)
                .setParameter("end", endTime)
                .getResultList();
        return list.stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Booking> findByResponsibleUserId(UUID userId) {
        List<DBBooking> list = em.createQuery("SELECT b FROM DBBooking b WHERE b.responsibleUser.id = :userId", DBBooking.class)
                .setParameter("userId", userId)
                .getResultList();
        return list.stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Booking> findByBookingSeriesId(UUID bookingSeriesId) {
        List<DBBooking> list = em.createQuery("SELECT b FROM DBBooking b WHERE b.bookingSeries.id = :seriesId", DBBooking.class)
                .setParameter("seriesId", bookingSeriesId)
                .getResultList();
        return list.stream().map(mapper::toDomain).collect(Collectors.toList());
    }
}
