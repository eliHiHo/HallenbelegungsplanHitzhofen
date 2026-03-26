package de.hallenbelegung.adapters.out.persistance.repository;

import de.hallenbelegung.adapters.out.persistance.entity.DBAuthSession;
import de.hallenbelegung.adapters.out.persistance.entity.DBUser;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.out.SessionPort;
import de.hallenbelegung.application.domain.view.SessionUserView;
import de.hallenbelegung.application.domain.port.out.PasswordHashingPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class JpaSessionRepository implements SessionPort {

    @Inject
    EntityManager em;

    @Inject
    PasswordHashingPort hashing;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String createSession(User user, Duration inactivityTimeout) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String tokenHash = hashing.hash(token);

        DBAuthSession entity = new DBAuthSession();
        entity.setUser(em.find(DBUser.class, user.getId()));
        entity.setSessionTokenHash(tokenHash);
        Instant now = Instant.now();
        entity.setLastAccessAt(now);
        entity.setExpiresAt(now.plus(inactivityTimeout));
        entity.setInvalidatedAt(null);

        em.persist(entity);
        em.flush();

        return token;
    }

    @Override
    public Optional<SessionUserView> findActiveSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return Optional.empty();
        String tokenHash = hashing.hash(sessionId);

        DBAuthSession entity = em.createQuery("SELECT s FROM DBAuthSession s WHERE s.sessionTokenHash = :h", DBAuthSession.class)
                .setParameter("h", tokenHash)
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (entity == null) return Optional.empty();

        Instant now = Instant.now();
        if (entity.getInvalidatedAt() != null) return Optional.empty();
        if (entity.getExpiresAt() == null || entity.getExpiresAt().isBefore(now)) return Optional.empty();

        return Optional.of(new SessionUserView(sessionId, entity.getUser().getId()));
    }

    @Override
    public void invalidateSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return;
        String tokenHash = hashing.hash(sessionId);

        DBAuthSession entity = em.createQuery("SELECT s FROM DBAuthSession s WHERE s.sessionTokenHash = :h", DBAuthSession.class)
                .setParameter("h", tokenHash)
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (entity == null) return;

        entity.setInvalidatedAt(Instant.now());
        em.merge(entity);
    }

    @Override
    public void invalidateSessionsByUserId(UUID userId) {
        if (userId == null) return;

        em.createQuery("UPDATE DBAuthSession s SET s.invalidatedAt = :now WHERE s.user.id = :uid AND s.invalidatedAt IS NULL")
                .setParameter("now", Instant.now())
                .setParameter("uid", userId)
                .executeUpdate();
    }

    @Override
    public void touchSession(String sessionId, Duration inactivityTimeout) {
        if (sessionId == null || sessionId.isBlank()) return;
        String tokenHash = hashing.hash(sessionId);

        DBAuthSession entity = em.createQuery("SELECT s FROM DBAuthSession s WHERE s.sessionTokenHash = :h", DBAuthSession.class)
                .setParameter("h", tokenHash)
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (entity == null) return;

        Instant now = Instant.now();
        entity.setLastAccessAt(now);
        entity.setExpiresAt(now.plus(inactivityTimeout));
        em.merge(entity);
    }
}
