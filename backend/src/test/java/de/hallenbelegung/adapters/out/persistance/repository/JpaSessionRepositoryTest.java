package de.hallenbelegung.adapters.out.persistance.repository;

import de.hallenbelegung.adapters.out.persistance.entity.DBAuthSession;
import de.hallenbelegung.adapters.out.persistance.entity.DBUser;
import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.out.TokenHashingPort;
import de.hallenbelegung.application.domain.view.SessionUserView;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaSessionRepositoryTest {

    private EntityManager em;
    private TokenHashingPort hashing;
    private JpaSessionRepository repository;

    @BeforeEach
    void setUp() {
        em = mock(EntityManager.class);
        hashing = mock(TokenHashingPort.class);

        repository = new JpaSessionRepository();
        repository.em = em;
        repository.hashing = hashing;
    }

    private User domainUser(UUID id) {
        return new User(id, "Max", "Mustermann", "max@example.com", "hash", Role.ADMIN, true, Instant.now(), Instant.now());
    }

    private DBUser dbUser(UUID id) {
        DBUser user = new DBUser();
        user.setId(id);
        user.setEmail("max@example.com");
        user.setPasswordHash("hash");
        user.setFirstName("Max");
        user.setLastName("Mustermann");
        user.setRole(Role.ADMIN);
        user.setActive(true);
        return user;
    }

    @Test
    void createSession_persists_session_and_returns_raw_token() {
        UUID userId = UUID.randomUUID();
        when(em.find(DBUser.class, userId)).thenReturn(dbUser(userId));
        when(hashing.hash(anyString())).thenReturn("token-hash");

        String token = repository.createSession(domainUser(userId), Duration.ofMinutes(30));

        assertNotNull(token);
        assertFalse(token.isBlank());

        verify(em).persist(any(DBAuthSession.class));
        verify(em).flush();
    }

    @Test
    void findActiveSession_returns_empty_for_blank_session_id() {
        Optional<SessionUserView> result = repository.findActiveSession("  ");

        assertTrue(result.isEmpty());
        verify(hashing, never()).hash(anyString());
    }

    @Test
    void findActiveSession_returns_empty_for_missing_session() {
        @SuppressWarnings("unchecked")
        TypedQuery<DBAuthSession> query = mock(TypedQuery.class);
        when(hashing.hash("token")).thenReturn("token-hash");
        when(em.createQuery(anyString(), eq(DBAuthSession.class))).thenReturn(query);
        when(query.setParameter(eq("h"), eq("token-hash"))).thenReturn(query);
        when(query.getResultStream()).thenReturn(Stream.empty());

        Optional<SessionUserView> result = repository.findActiveSession("token");

        assertTrue(result.isEmpty());
    }

    @Test
    void findActiveSession_returns_empty_for_invalidated_or_expired_session() {
        DBAuthSession invalidated = new DBAuthSession();
        invalidated.setUser(dbUser(UUID.randomUUID()));
        invalidated.setExpiresAt(Instant.now().plusSeconds(100));
        invalidated.setInvalidatedAt(Instant.now());

        @SuppressWarnings("unchecked")
        TypedQuery<DBAuthSession> query = mock(TypedQuery.class);
        when(hashing.hash("token")).thenReturn("token-hash");
        when(em.createQuery(anyString(), eq(DBAuthSession.class))).thenReturn(query);
        when(query.setParameter(eq("h"), eq("token-hash"))).thenReturn(query);
        when(query.getResultStream()).thenReturn(Stream.of(invalidated));

        assertTrue(repository.findActiveSession("token").isEmpty());

        DBAuthSession expired = new DBAuthSession();
        expired.setUser(dbUser(UUID.randomUUID()));
        expired.setInvalidatedAt(null);
        expired.setExpiresAt(Instant.now().minusSeconds(1));
        when(query.getResultStream()).thenReturn(Stream.of(expired));

        assertTrue(repository.findActiveSession("token").isEmpty());
    }

    @Test
    void findActiveSession_returns_view_for_valid_session() {
        UUID userId = UUID.randomUUID();

        DBAuthSession session = new DBAuthSession();
        session.setUser(dbUser(userId));
        session.setInvalidatedAt(null);
        session.setExpiresAt(Instant.now().plusSeconds(300));

        @SuppressWarnings("unchecked")
        TypedQuery<DBAuthSession> query = mock(TypedQuery.class);
        when(hashing.hash("token")).thenReturn("token-hash");
        when(em.createQuery(anyString(), eq(DBAuthSession.class))).thenReturn(query);
        when(query.setParameter(eq("h"), eq("token-hash"))).thenReturn(query);
        when(query.getResultStream()).thenReturn(Stream.of(session));

        Optional<SessionUserView> result = repository.findActiveSession("token");

        assertTrue(result.isPresent());
        assertEquals("token", result.get().getSessionId());
        assertEquals(userId, result.get().getUserId());
    }

    @Test
    void invalidateSession_marks_existing_session() {
        DBAuthSession session = new DBAuthSession();

        @SuppressWarnings("unchecked")
        TypedQuery<DBAuthSession> query = mock(TypedQuery.class);
        when(hashing.hash("token")).thenReturn("token-hash");
        when(em.createQuery(anyString(), eq(DBAuthSession.class))).thenReturn(query);
        when(query.setParameter(eq("h"), eq("token-hash"))).thenReturn(query);
        when(query.getResultStream()).thenReturn(Stream.of(session));

        repository.invalidateSession("token");

        assertNotNull(session.getInvalidatedAt());
        verify(em).merge(session);
    }

    @Test
    void invalidateSessionsByUserId_executes_bulk_update() {
        Query query = mock(Query.class);
        UUID userId = UUID.randomUUID();

        when(em.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(eq("now"), any(Instant.class))).thenReturn(query);
        when(query.setParameter("uid", userId)).thenReturn(query);

        repository.invalidateSessionsByUserId(userId);

        verify(query).executeUpdate();
    }

    @Test
    void touchSession_updates_last_access_and_expiry() {
        DBAuthSession session = new DBAuthSession();

        @SuppressWarnings("unchecked")
        TypedQuery<DBAuthSession> query = mock(TypedQuery.class);
        when(hashing.hash("token")).thenReturn("token-hash");
        when(em.createQuery(anyString(), eq(DBAuthSession.class))).thenReturn(query);
        when(query.setParameter(eq("h"), eq("token-hash"))).thenReturn(query);
        when(query.getResultStream()).thenReturn(Stream.of(session));

        repository.touchSession("token", Duration.ofMinutes(10));

        assertNotNull(session.getLastAccessAt());
        assertNotNull(session.getExpiresAt());
        verify(em).merge(session);
    }
}

