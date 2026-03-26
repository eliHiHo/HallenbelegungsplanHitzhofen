package de.hallenbelegung.adapters.out.persistance.repository;

import de.hallenbelegung.adapters.out.persistance.entity.DBBookingSeries;
import de.hallenbelegung.adapters.out.persistance.mapper.BookingSeriesPersistenceMapper;
import de.hallenbelegung.adapters.out.persistance.mapper.HallPersistenceMapper;
import de.hallenbelegung.adapters.out.persistance.mapper.UserPersistenceMapper;
import de.hallenbelegung.application.domain.model.BookingSeries;
import de.hallenbelegung.application.domain.port.out.BookingSeriesRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class JpaBookingSeriesRepository implements BookingSeriesRepositoryPort {

    @Inject
    EntityManager em;

    private final BookingSeriesPersistenceMapper mapper = new BookingSeriesPersistenceMapper(new HallPersistenceMapper(), new UserPersistenceMapper());

    @Override
    public BookingSeries save(BookingSeries bookingSeries) {
        DBBookingSeries entity = mapper.toEntity(bookingSeries);
        DBBookingSeries merged = em.merge(entity);
        return mapper.toDomain(merged);
    }

    @Override
    public Optional<BookingSeries> findById(UUID bookingSeriesId) {
        DBBookingSeries entity = em.find(DBBookingSeries.class, bookingSeriesId);
        return Optional.ofNullable(mapper.toDomain(entity));
    }

    @Override
    public List<BookingSeries> findByHallId(UUID hallId) {
        List<DBBookingSeries> list = em.createQuery("SELECT s FROM DBBookingSeries s WHERE s.hall.id = :hallId", DBBookingSeries.class)
                .setParameter("hallId", hallId)
                .getResultList();
        return list.stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<BookingSeries> findActiveByHallIdAndDateRange(UUID hallId, LocalDate startDate, LocalDate endDate) {
        List<DBBookingSeries> list = em.createQuery("SELECT s FROM DBBookingSeries s WHERE s.hall.id = :hallId AND s.startDate <= :endDate AND s.endDate >= :startDate", DBBookingSeries.class)
                .setParameter("hallId", hallId)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getResultList();
        return list.stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<BookingSeries> findByResponsibleUserId(UUID userId) {
        List<DBBookingSeries> list = em.createQuery("SELECT s FROM DBBookingSeries s WHERE s.responsibleUser.id = :userId", DBBookingSeries.class)
                .setParameter("userId", userId)
                .getResultList();
        return list.stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<BookingSeries> findAll() {
        List<DBBookingSeries> list = em.createQuery("SELECT s FROM DBBookingSeries s", DBBookingSeries.class).getResultList();
        return list.stream().map(mapper::toDomain).collect(Collectors.toList());
    }
}
