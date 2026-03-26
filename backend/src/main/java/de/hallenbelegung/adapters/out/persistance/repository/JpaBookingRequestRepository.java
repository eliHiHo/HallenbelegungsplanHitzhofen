package de.hallenbelegung.adapters.out.persistance.repository;

import de.hallenbelegung.adapters.out.persistance.entity.DBBookingRequest;
import de.hallenbelegung.adapters.out.persistance.mapper.BookingRequestPersistenceMapper;
import de.hallenbelegung.adapters.out.persistance.mapper.HallPersistenceMapper;
import de.hallenbelegung.adapters.out.persistance.mapper.UserPersistenceMapper;
import de.hallenbelegung.application.domain.model.BookingRequest;
import de.hallenbelegung.application.domain.model.BookingRequestStatus;
import de.hallenbelegung.application.domain.port.out.BookingRequestRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class JpaBookingRequestRepository implements BookingRequestRepositoryPort {

    @Inject
    EntityManager em;

    private final BookingRequestPersistenceMapper mapper = new BookingRequestPersistenceMapper(new HallPersistenceMapper(), new UserPersistenceMapper());

    @Override
    public BookingRequest save(BookingRequest bookingRequest) {
        DBBookingRequest entity = mapper.toEntity(bookingRequest);
        DBBookingRequest merged = em.merge(entity);
        return mapper.toDomain(merged);
    }

    @Override
    public Optional<BookingRequest> findById(UUID bookingRequestId) {
        DBBookingRequest entity = em.find(DBBookingRequest.class, bookingRequestId);
        return Optional.ofNullable(mapper.toDomain(entity));
    }

    @Override
    public List<BookingRequest> findAll() {
        List<DBBookingRequest> list = em.createQuery("SELECT r FROM DBBookingRequest r", DBBookingRequest.class).getResultList();
        return list.stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<BookingRequest> findByStatus(BookingRequestStatus status) {
        List<DBBookingRequest> list = em.createQuery("SELECT r FROM DBBookingRequest r WHERE r.status = :status", DBBookingRequest.class)
                .setParameter("status", status)
                .getResultList();
        return list.stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<BookingRequest> findByRequestingUserId(UUID userId) {
        List<DBBookingRequest> list = em.createQuery("SELECT r FROM DBBookingRequest r WHERE r.requestedBy.id = :userId", DBBookingRequest.class)
                .setParameter("userId", userId)
                .getResultList();
        return list.stream().map(mapper::toDomain).collect(Collectors.toList());
    }

}
