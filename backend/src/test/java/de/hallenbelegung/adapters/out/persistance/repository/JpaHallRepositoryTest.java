package de.hallenbelegung.adapters.out.persistance.repository;

import de.hallenbelegung.adapters.out.persistance.entity.DBHall;
import de.hallenbelegung.application.domain.model.Hall;
import de.hallenbelegung.application.domain.model.HallType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaHallRepositoryTest {

    private EntityManager em;
    private JpaHallRepository repository;

    @BeforeEach
    void setUp() {
        em = mock(EntityManager.class);
        repository = new JpaHallRepository();
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

    private Hall domainHall(UUID id, boolean active, HallType type) {
        return new Hall(id, "Halle A", "desc", active, Instant.now(), Instant.now(), type);
    }

    private DBHall dbHall(UUID id, boolean active, HallType type) {
        DBHall h = new DBHall();
        h.setId(id);
        h.setName("Halle A");
        h.setDescription("desc");
        h.setActive(active);
        h.setHallType(type);
        setField(h, "createdAt", Instant.now());
        setField(h, "updatedAt", Instant.now());
        return h;
    }

    @Test
    void save_new_hall_uses_persist_and_flush() {
        Hall input = domainHall(null, true, HallType.PART_SMALL);

        doAnswer(invocation -> {
            DBHall entity = invocation.getArgument(0);
            setField(entity, "createdAt", Instant.now());
            setField(entity, "updatedAt", Instant.now());
            return null;
        }).when(em).persist(any(DBHall.class));

        repository.save(input);

        verify(em).persist(any(DBHall.class));
        verify(em).flush();
    }

    @Test
    void save_existing_hall_uses_merge_and_flush() {
        UUID id = UUID.randomUUID();
        Hall input = domainHall(id, true, HallType.FULL);
        when(em.merge(any(DBHall.class))).thenReturn(dbHall(id, true, HallType.FULL));

        repository.save(input);

        verify(em).merge(any(DBHall.class));
        verify(em).flush();
    }

    @Test
    void findById_returns_hit_and_miss() {
        UUID id = UUID.randomUUID();
        when(em.find(DBHall.class, id)).thenReturn(dbHall(id, true, HallType.PART_SMALL));

        Optional<Hall> found = repository.findById(id);
        assertTrue(found.isPresent());
        assertEquals(id, found.get().getId());

        when(em.find(DBHall.class, id)).thenReturn(null);
        assertFalse(repository.findById(id).isPresent());
    }

    @Test
    void findAll_and_findAllActive_map_queries() {
        @SuppressWarnings("unchecked")
        TypedQuery<DBHall> queryAll = mock(TypedQuery.class);
        @SuppressWarnings("unchecked")
        TypedQuery<DBHall> queryActive = mock(TypedQuery.class);

        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        when(em.createQuery("SELECT h FROM DBHall h", DBHall.class)).thenReturn(queryAll);
        when(queryAll.getResultList()).thenReturn(List.of(dbHall(id1, true, HallType.PART_SMALL), dbHall(id2, false, HallType.FULL)));

        when(em.createQuery("SELECT h FROM DBHall h WHERE h.active = true", DBHall.class)).thenReturn(queryActive);
        when(queryActive.getResultList()).thenReturn(List.of(dbHall(id1, true, HallType.PART_SMALL)));

        List<Hall> all = repository.findAll();
        List<Hall> active = repository.findAllActive();

        assertEquals(2, all.size());
        assertEquals(1, active.size());
        assertEquals(id1, active.get(0).getId());
    }
}

