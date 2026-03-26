package de.hallenbelegung.adapters.out.security;

import de.hallenbelegung.application.domain.port.out.LoginRateLimitPort;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class LoginRateLimitInMemory implements LoginRateLimitPort {

    // Simple settings
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration FAILED_WINDOW = Duration.ofMinutes(15);
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private static final int MAX_PASSWORD_RESET = 3;
    private static final Duration RESET_WINDOW = Duration.ofHours(1);

    private final Map<String, Deque<Instant>> failedLogins = new ConcurrentHashMap<>();
    private final Map<String, Instant> lockedUntil = new ConcurrentHashMap<>();
    private final Map<String, Deque<Instant>> passwordResetRequests = new ConcurrentHashMap<>();

    @Override
    public void checkLoginAllowed(String key) {
        Instant now = Instant.now();
        Instant locked = lockedUntil.get(key);
        if (locked != null && locked.isAfter(now)) {
            throw new RuntimeException("Too many failed login attempts");
        }
    }

    @Override
    public void recordFailedLogin(String key) {
        Instant now = Instant.now();
        failedLogins.computeIfAbsent(key, k -> new ArrayDeque<>()).addLast(now);
        Deque<Instant> deque = failedLogins.get(key);

        // purge old
        while (!deque.isEmpty() && deque.peekFirst().isBefore(now.minus(FAILED_WINDOW))) {
            deque.removeFirst();
        }

        if (deque.size() >= MAX_FAILED_ATTEMPTS) {
            lockedUntil.put(key, now.plus(LOCK_DURATION));
            deque.clear();
        }
    }

    @Override
    public void resetLoginFailures(String key) {
        failedLogins.remove(key);
        lockedUntil.remove(key);
    }

    @Override
    public void checkPasswordResetAllowed(String key) {
        Instant now = Instant.now();
        Deque<Instant> deque = passwordResetRequests.get(key);
        if (deque == null) return;
        while (!deque.isEmpty() && deque.peekFirst().isBefore(now.minus(RESET_WINDOW))) {
            deque.removeFirst();
        }
        if (deque.size() >= MAX_PASSWORD_RESET) {
            throw new RuntimeException("Too many password reset requests");
        }
    }

    @Override
    public void recordPasswordResetRequest(String key) {
        Instant now = Instant.now();
        passwordResetRequests.computeIfAbsent(key, k -> new ArrayDeque<>()).addLast(now);
        Deque<Instant> deque = passwordResetRequests.get(key);
        while (!deque.isEmpty() && deque.peekFirst().isBefore(now.minus(RESET_WINDOW))) {
            deque.removeFirst();
        }
    }
}
