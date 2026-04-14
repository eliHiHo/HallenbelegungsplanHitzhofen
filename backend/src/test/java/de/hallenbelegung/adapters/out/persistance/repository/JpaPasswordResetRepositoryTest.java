package de.hallenbelegung.adapters.out.persistance.repository;

import de.hallenbelegung.adapters.out.persistance.entity.DBPasswordResetToken;
import de.hallenbelegung.adapters.out.persistance.entity.DBUser;
import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.port.out.TokenHashingPort;
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

class JpaPasswordResetRepositoryTest {

    private EntityManager em;
    private TokenHashingPort hashing;
    private JpaPasswordResetRepository repository;

    @BeforeEach
    void setUp() {
        em = mock(EntityManager.class);
        hashing = mock(TokenHashingPort.class);

        repository = new JpaPasswordResetRepository();
        repository.em = em;
        repository.hashing = hashing;
    }

    private DBUser dbUser(UUID id) {
        DBUser user = new DBUser();
        user.setId(id);
        user.setEmail("max@example.com");
        user.setPasswordHash("hash");
        user.setFirstName("Max");
        user.setLastName("Mustermann");
        user.setRole(Role.CLUB_REPRESENTATIVE);
        user.setActive(true);
        return user;
    }

    @Test
    void createToken_persists_token_and_returns_raw_token() {
        UUID userId = UUID.randomUUID();
        when(em.find(DBUser.class, userId)).thenReturn(dbUser(userId));
        when(hashing.hash(anyString())).thenReturn("token-hash");

        String token = repository.createToken(userId, Duration.ofMinutes(20));

        assertNotNull(token);
        assertFalse(token.isBlank());
        verify(em).persist(any(DBPasswordResetToken.class));
        verify(em).flush();
    }

    @Test
    void findUserIdByValidToken_returns_empty_for_blank_input() {
        Optional<UUID> result = repository.findUserIdByValidToken("  ");

        assertTrue(result.isEmpty());
        verify(hashing, never()).hash(anyString());
    }

    @Test
    void findUserIdByValidToken_returns_empty_for_missing_or_invalid_token() {
        @SuppressWarnings("unchecked")
        TypedQuery<DBPasswordResetToken> query = mock(TypedQuery.class);
        when(hashing.hash("token")).thenReturn("token-hash");
        when(em.createQuery(anyString(), eq(DBPasswordResetToken.class))).thenReturn(query);
        when(query.setParameter(eq("h"), eq("token-hash"))).thenReturn(query);
        when(query.getResultStream()).thenReturn(Stream.empty());

        assertTrue(repository.findUserIdByValidToken("token").isEmpty());

        DBPasswordResetToken used = new DBPasswordResetToken();
        used.setUser(dbUser(UUID.randomUUID()));
        used.setUsedAt(Instant.now());
        used.setExpiresAt(Instant.now().plusSeconds(60));
        when(query.getResultStream()).thenReturn(Stream.of(used));
        assertTrue(repository.findUserIdByValidToken("token").isEmpty());

        DBPasswordResetToken expired = new DBPasswordResetToken();
        expired.setUser(dbUser(UUID.randomUUID()));
        expired.setUsedAt(null);
        expired.setExpiresAt(Instant.now().minusSeconds(1));
        when(query.getResultStream()).thenReturn(Stream.of(expired));
        assertTrue(repository.findUserIdByValidToken("token").isEmpty());
    }

    @Test
    void findUserIdByValidToken_returns_user_id_for_valid_token() {
        UUID userId = UUID.randomUUID();
        DBPasswordResetToken tokenEntity = new DBPasswordResetToken();
        tokenEntity.setUser(dbUser(userId));
        tokenEntity.setUsedAt(null);
        tokenEntity.setExpiresAt(Instant.now().plusSeconds(300));

        @SuppressWarnings("unchecked")
        TypedQuery<DBPasswordResetToken> query = mock(TypedQuery.class);
        when(hashing.hash("token")).thenReturn("token-hash");
        when(em.createQuery(anyString(), eq(DBPasswordResetToken.class))).thenReturn(query);
        when(query.setParameter(eq("h"), eq("token-hash"))).thenReturn(query);
        when(query.getResultStream()).thenReturn(Stream.of(tokenEntity));

        Optional<UUID> result = repository.findUserIdByValidToken("token");

        assertTrue(result.isPresent());
        assertEquals(userId, result.get());
    }

    @Test
    void invalidateToken_marks_token_as_used_when_found() {
        DBPasswordResetToken tokenEntity = new DBPasswordResetToken();

        @SuppressWarnings("unchecked")
        TypedQuery<DBPasswordResetToken> query = mock(TypedQuery.class);
        when(hashing.hash("token")).thenReturn("token-hash");
        when(em.createQuery(anyString(), eq(DBPasswordResetToken.class))).thenReturn(query);
        when(query.setParameter(eq("h"), eq("token-hash"))).thenReturn(query);
        when(query.getResultStream()).thenReturn(Stream.of(tokenEntity));

        repository.invalidateToken("token");

        assertNotNull(tokenEntity.getUsedAt());
        verify(em).merge(tokenEntity);
    }

    @Test
    void invalidateTokensByUserId_executes_bulk_update() {
        Query query = mock(Query.class);
        UUID userId = UUID.randomUUID();

        when(em.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(eq("now"), any(Instant.class))).thenReturn(query);
        when(query.setParameter("uid", userId)).thenReturn(query);

        repository.invalidateTokensByUserId(userId);

        verify(query).executeUpdate();
    }
}

