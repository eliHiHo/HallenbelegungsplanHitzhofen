package de.hallenbelegung.adapters.out.persistance.repository;

import de.hallenbelegung.adapters.out.persistance.entity.DBBookingSeriesRequest;
import de.hallenbelegung.adapters.out.persistance.mapper.BookingSeriesRequestPersistenceMapper;
import de.hallenbelegung.adapters.out.persistance.mapper.HallPersistenceMapper;
import de.hallenbelegung.adapters.out.persistance.mapper.UserPersistenceMapper;
import de.hallenbelegung.application.domain.model.BookingSeriesRequest;
import de.hallenbelegung.application.domain.model.BookingRequestStatus;
import de.hallenbelegung.application.domain.port.out.BookingSeriesRequestRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class JpaBookingSeriesRequestRepository implements BookingSeriesRequestRepositoryPort {

    @Inject
    EntityManager em;

    private final BookingSeriesRequestPersistenceMapper mapper = new BookingSeriesRequestPersistenceMapper(
            new HallPersistenceMapper(), new UserPersistenceMapper()
    );

    @Override
    public BookingSeriesRequest save(BookingSeriesRequest bookingSeriesRequest) {
        DBBookingSeriesRequest entity = mapper.toEntity(bookingSeriesRequest);
        DBBookingSeriesRequest merged = em.merge(entity);
        return mapper.toDomain(merged);
    }

    @Override
    public Optional<BookingSeriesRequest> findById(UUID bookingSeriesRequestId) {
        DBBookingSeriesRequest entity = em.find(DBBookingSeriesRequest.class, bookingSeriesRequestId);
        return Optional.ofNullable(mapper.toDomain(entity));
    }

    @Override
    public List<BookingSeriesRequest> findAll() {
        List<DBBookingSeriesRequest> list = em.createQuery("SELECT r FROM DBBookingSeriesRequest r", DBBookingSeriesRequest.class).getResultList();
        return list.stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<BookingSeriesRequest> findByStatus(BookingRequestStatus status) {
        List<DBBookingSeriesRequest> list = em.createQuery("SELECT r FROM DBBookingSeriesRequest r WHERE r.status = :status", DBBookingSeriesRequest.class)
                .setParameter("status", status)
                .getResultList();
        return list.stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<BookingSeriesRequest> findByRequestedByUserId(UUID userId) {
        List<DBBookingSeriesRequest> list = em.createQuery("SELECT r FROM DBBookingSeriesRequest r WHERE r.requestedBy.id = :userId", DBBookingSeriesRequest.class)
                .setParameter("userId", userId)
                .getResultList();
        return list.stream().map(mapper::toDomain).collect(Collectors.toList());
    }
}
