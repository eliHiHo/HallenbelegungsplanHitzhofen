package de.hallenbelegung.adapters.out.persistance.repository;

import de.hallenbelegung.adapters.out.persistance.entity.DBBlockedTime;
import de.hallenbelegung.adapters.out.persistance.mapper.BlockedTimePersistenceMapper;
import de.hallenbelegung.adapters.out.persistance.mapper.HallPersistenceMapper;
import de.hallenbelegung.adapters.out.persistance.mapper.UserPersistenceMapper;
import de.hallenbelegung.application.domain.model.BlockedTime;
import de.hallenbelegung.application.domain.port.out.BlockedTimeRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class JpaBlockedTimeRepository implements BlockedTimeRepositoryPort {

    @Inject
    EntityManager em;

    private final BlockedTimePersistenceMapper mapper = new BlockedTimePersistenceMapper(new HallPersistenceMapper(), new UserPersistenceMapper());

    @Override
    public BlockedTime save(BlockedTime blockedTime) {
        DBBlockedTime entity = mapper.toEntity(blockedTime);
        DBBlockedTime merged = em.merge(entity);
        em.flush(); // required: @CreationTimestamp/@UpdateTimestamp are set during flush
        return mapper.toDomain(merged);
    }

    @Override
    public Optional<BlockedTime> findById(UUID blockedTimeId) {
        DBBlockedTime entity = em.find(DBBlockedTime.class, blockedTimeId);
        return Optional.ofNullable(mapper.toDomain(entity));
    }

    @Override
    public List<BlockedTime> findByHallIdAndTimeRange(UUID hallId, LocalDateTime startTime, LocalDateTime endTime) {
        // overlap logic for blocked times on a hall
        List<DBBlockedTime> list = em.createQuery("SELECT b FROM DBBlockedTime b WHERE b.hall.id = :hallId AND b.startAt < :end AND b.endAt > :start", DBBlockedTime.class)
                .setParameter("hallId", hallId)
                .setParameter("start", startTime)
                .setParameter("end", endTime)
                .getResultList();
        return list.stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<BlockedTime> findAllByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        // overlap logic for blocked times in general
        List<DBBlockedTime> list = em.createQuery("SELECT b FROM DBBlockedTime b WHERE b.startAt < :end AND b.endAt > :start", DBBlockedTime.class)
                .setParameter("start", startTime)
                .setParameter("end", endTime)
                .getResultList();
        return list.stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID blockedTimeId) {
        DBBlockedTime entity = em.find(DBBlockedTime.class, blockedTimeId);
        if (entity != null) em.remove(entity);
    }

    @Override
    public List<BlockedTime> findAll() {
        List<DBBlockedTime> list = em.createQuery("SELECT b FROM DBBlockedTime b", DBBlockedTime.class).getResultList();
        return list.stream().map(mapper::toDomain).collect(Collectors.toList());
    }
}
