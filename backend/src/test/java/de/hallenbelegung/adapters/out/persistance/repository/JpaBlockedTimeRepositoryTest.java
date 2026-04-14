package de.hallenbelegung.adapters.out.persistance.repository;

import de.hallenbelegung.adapters.out.persistance.entity.DBBlockedTime;
import de.hallenbelegung.adapters.out.persistance.entity.DBHall;
import de.hallenbelegung.adapters.out.persistance.entity.DBUser;
import de.hallenbelegung.application.domain.model.BlockedTime;
import de.hallenbelegung.application.domain.model.BlockedTimeType;
import de.hallenbelegung.application.domain.model.HallType;
import de.hallenbelegung.application.domain.model.Role;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaBlockedTimeRepositoryTest {

    private EntityManager em;
    private JpaBlockedTimeRepository repository;

    @BeforeEach
    void setUp() {
        em = mock(EntityManager.class);
        repository = new JpaBlockedTimeRepository();
        repository.em = em;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private DBUser dbUser(UUID id) {
        DBUser u = new DBUser();
        u.setId(id);
        u.setFirstName("Admin");
        u.setLastName("User");
        u.setEmail("admin@example.com");
        u.setPasswordHash("hash");
        u.setRole(Role.ADMIN);
        u.setActive(true);
        setField(u, "createdAt", Instant.now());
        setField(u, "updatedAt", Instant.now());
        return u;
    }

    private DBHall dbHall(UUID id) {
        DBHall h = new DBHall();
        h.setId(id);
        h.setName("Halle A");
        h.setDescription("desc");
        h.setHallType(HallType.PART_SMALL);
        h.setActive(true);
        setField(h, "createdAt", Instant.now());
        setField(h, "updatedAt", Instant.now());
        return h;
    }

    private DBBlockedTime dbBlockedTime(UUID id) {
        DBBlockedTime b = new DBBlockedTime();
        b.setId(id);
        b.setReason("Wartung");
        b.setStartAt(LocalDateTime.of(2026, 5, 4, 8, 0));
        b.setEndAt(LocalDateTime.of(2026, 5, 4, 12, 0));
        b.setType(BlockedTimeType.MANUAL);
        b.setHall(dbHall(UUID.randomUUID()));
        b.setCreatedBy(dbUser(UUID.randomUUID()));
        b.setUpdatedBy(null);
        setField(b, "createdAt", Instant.now());
        setField(b, "updatedAt", Instant.now());
        return b;
    }

    @Test
    void findByHallIdAndTimeRange_binds_parameters_and_maps() {
        @SuppressWarnings("unchecked")
        TypedQuery<DBBlockedTime> query = mock(TypedQuery.class);
        UUID id = UUID.randomUUID();
        UUID hallId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.of(2026, 5, 4, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 5, 4, 10, 0);

        when(em.createQuery(anyString(), eq(DBBlockedTime.class))).thenReturn(query);
        when(query.setParameter("hallId", hallId)).thenReturn(query);
        when(query.setParameter("start", start)).thenReturn(query);
        when(query.setParameter("end", end)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(dbBlockedTime(id)));

        List<BlockedTime> result = repository.findByHallIdAndTimeRange(hallId, start, end);

        assertEquals(1, result.size());
        assertEquals(id, result.get(0).getId());
    }

    @Test
    void findAllByTimeRange_binds_parameters_and_maps() {
        @SuppressWarnings("unchecked")
        TypedQuery<DBBlockedTime> query = mock(TypedQuery.class);
        UUID id = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.of(2026, 5, 4, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 5, 4, 10, 0);

        when(em.createQuery(anyString(), eq(DBBlockedTime.class))).thenReturn(query);
        when(query.setParameter("start", start)).thenReturn(query);
        when(query.setParameter("end", end)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(dbBlockedTime(id)));

        List<BlockedTime> result = repository.findAllByTimeRange(start, end);

        assertEquals(1, result.size());
        assertEquals(id, result.get(0).getId());
    }

    @Test
    void deleteById_removes_existing_and_ignores_missing() {
        UUID id = UUID.randomUUID();
        DBBlockedTime entity = dbBlockedTime(id);

        when(em.find(DBBlockedTime.class, id)).thenReturn(entity);
        repository.deleteById(id);
        verify(em).remove(entity);

        when(em.find(DBBlockedTime.class, id)).thenReturn(null);
        repository.deleteById(id);
        verify(em, never()).remove((DBBlockedTime) null);
    }
}

