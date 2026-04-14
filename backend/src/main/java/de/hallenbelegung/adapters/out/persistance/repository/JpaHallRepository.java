package de.hallenbelegung.adapters.out.persistance.repository;

import de.hallenbelegung.adapters.out.persistance.entity.DBHall;
import de.hallenbelegung.adapters.out.persistance.mapper.HallPersistenceMapper;
import de.hallenbelegung.application.domain.model.Hall;
import de.hallenbelegung.application.domain.port.out.HallRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class JpaHallRepository implements HallRepositoryPort {

    @Inject
    EntityManager em;

    private final HallPersistenceMapper mapper = new HallPersistenceMapper();

    @Override
    @Transactional
    public Hall save(Hall hall) {
        DBHall entity = mapper.toEntity(hall);

        if (hall.getId() == null) {
            em.persist(entity);
            em.flush();
            return mapper.toDomain(entity);
        }

        DBHall merged = em.merge(entity);
        em.flush();
        return mapper.toDomain(merged);
    }

    @Override
    public Optional<Hall> findById(UUID hallId) {
        DBHall entity = em.find(DBHall.class, hallId);
        return Optional.ofNullable(mapper.toDomain(entity));
    }

    @Override
    public List<Hall> findAll() {
        List<DBHall> list = em.createQuery("SELECT h FROM DBHall h", DBHall.class).getResultList();
        return list.stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Hall> findAllActive() {
        List<DBHall> list = em.createQuery("SELECT h FROM DBHall h WHERE h.active = true", DBHall.class).getResultList();
        return list.stream().map(mapper::toDomain).collect(Collectors.toList());
    }
}
