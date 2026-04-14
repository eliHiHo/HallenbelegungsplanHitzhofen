package de.hallenbelegung.adapters.out.persistance.repository;

import de.hallenbelegung.adapters.out.persistance.entity.DBUser;
import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.model.User;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

class JpaUserRepositoryTest {

    private EntityManager em;
    private JpaUserRepository repository;

    @BeforeEach
    void setUp() {
        em = mock(EntityManager.class);
        repository = new JpaUserRepository();
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

    private User domainUser(UUID id) {
        return new User(id, "Max", "Mustermann", "max@example.com", "hash", Role.ADMIN, true, Instant.now(), Instant.now());
    }

    private DBUser dbUser(UUID id, boolean active) {
        DBUser u = new DBUser();
        u.setId(id);
        u.setFirstName("Max");
        u.setLastName("Mustermann");
        u.setEmail("max@example.com");
        u.setPasswordHash("hash");
        u.setRole(Role.ADMIN);
        u.setActive(active);
        setField(u, "createdAt", Instant.now());
        setField(u, "updatedAt", Instant.now());
        return u;
    }

    @Test
    void save_new_user_uses_persist_and_flush() {
        User input = domainUser(null);

        doAnswer(invocation -> {
            DBUser entity = invocation.getArgument(0);
            setField(entity, "createdAt", Instant.now());
            setField(entity, "updatedAt", Instant.now());
            return null;
        }).when(em).persist(any(DBUser.class));

        repository.save(input);

        verify(em).persist(any(DBUser.class));
        verify(em).flush();
    }

    @Test
    void save_existing_user_uses_merge_and_flush() {
        UUID id = UUID.randomUUID();
        User input = domainUser(id);

        DBUser merged = dbUser(id, true);
        when(em.merge(any(DBUser.class))).thenReturn(merged);

        repository.save(input);

        verify(em).merge(any(DBUser.class));
        verify(em).flush();
    }

    @Test
    void findByEmail_returns_hit_and_miss() {
        @SuppressWarnings("unchecked")
        TypedQuery<DBUser> query = mock(TypedQuery.class);
        UUID id = UUID.randomUUID();

        when(em.createQuery(anyString(), eq(DBUser.class))).thenReturn(query);
        when(query.setParameter("email", "max@example.com")).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(dbUser(id, true)));

        Optional<User> found = repository.findByEmail("max@example.com");
        assertTrue(found.isPresent());
        assertEquals(id, found.get().getId());

        when(query.getResultList()).thenReturn(List.of());
        assertFalse(repository.findByEmail("max@example.com").isPresent());
    }

    @Test
    void findAllActive_maps_only_active_list() {
        @SuppressWarnings("unchecked")
        TypedQuery<DBUser> query = mock(TypedQuery.class);
        UUID id = UUID.randomUUID();

        when(em.createQuery("SELECT u FROM DBUser u WHERE u.active = true", DBUser.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(dbUser(id, true)));

        List<User> result = repository.findAllActive();

        assertEquals(1, result.size());
        assertEquals(id, result.get(0).getId());
    }
}

