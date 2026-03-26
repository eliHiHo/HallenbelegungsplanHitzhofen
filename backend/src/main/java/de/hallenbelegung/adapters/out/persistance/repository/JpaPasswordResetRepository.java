package de.hallenbelegung.adapters.out.persistance.repository;

import de.hallenbelegung.adapters.out.persistance.entity.DBPasswordResetToken;
import de.hallenbelegung.adapters.out.persistance.entity.DBUser;
import de.hallenbelegung.application.domain.port.out.PasswordResetPort;
import de.hallenbelegung.application.domain.port.out.TokenHashingPort;
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
public class JpaPasswordResetRepository implements PasswordResetPort {

    @Inject
    EntityManager em;

    @Inject
    TokenHashingPort hashing;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String createToken(UUID userId, Duration validity) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String tokenHash = hashing.hash(token);

        DBPasswordResetToken entity = new DBPasswordResetToken();
        entity.setUser(em.find(DBUser.class, userId));
        entity.setTokenHash(tokenHash);
        Instant now = Instant.now();
        entity.setExpiresAt(now.plus(validity));
        entity.setUsedAt(null);

        em.persist(entity);
        em.flush();

        return token;
    }

    @Override
    public Optional<UUID> findUserIdByValidToken(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        String tokenHash = hashing.hash(token);

        DBPasswordResetToken entity = em.createQuery("SELECT t FROM DBPasswordResetToken t WHERE t.tokenHash = :h", DBPasswordResetToken.class)
                .setParameter("h", tokenHash)
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (entity == null) return Optional.empty();

        Instant now = Instant.now();
        if (entity.getUsedAt() != null) return Optional.empty();
        if (entity.getExpiresAt() == null || entity.getExpiresAt().isBefore(now)) return Optional.empty();

        return Optional.of(entity.getUser().getId());
    }

    @Override
    public void invalidateToken(String token) {
        if (token == null || token.isBlank()) return;
        String tokenHash = hashing.hash(token);

        DBPasswordResetToken entity = em.createQuery("SELECT t FROM DBPasswordResetToken t WHERE t.tokenHash = :h", DBPasswordResetToken.class)
                .setParameter("h", tokenHash)
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (entity == null) return;

        entity.setUsedAt(Instant.now());
        em.merge(entity);
    }

    @Override
    public void invalidateTokensByUserId(UUID userId) {
        if (userId == null) return;

        em.createQuery("UPDATE DBPasswordResetToken t SET t.usedAt = :now WHERE t.user.id = :uid AND t.usedAt IS NULL")
                .setParameter("now", Instant.now())
                .setParameter("uid", userId)
                .executeUpdate();
    }
}
