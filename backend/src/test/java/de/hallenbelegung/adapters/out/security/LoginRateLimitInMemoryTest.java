package de.hallenbelegung.adapters.out.security;

import de.hallenbelegung.application.domain.exception.RateLimitException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class LoginRateLimitInMemoryTest {

    @SuppressWarnings("unchecked")
    private static Map<String, Deque<Instant>> failedLogins(LoginRateLimitInMemory limiter) {
        try {
            Field f = LoginRateLimitInMemory.class.getDeclaredField("failedLogins");
            f.setAccessible(true);
            return (Map<String, Deque<Instant>>) f.get(limiter);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Instant> lockedUntil(LoginRateLimitInMemory limiter) {
        try {
            Field f = LoginRateLimitInMemory.class.getDeclaredField("lockedUntil");
            f.setAccessible(true);
            return (Map<String, Instant>) f.get(limiter);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Deque<Instant>> passwordResetRequests(LoginRateLimitInMemory limiter) {
        try {
            Field f = LoginRateLimitInMemory.class.getDeclaredField("passwordResetRequests");
            f.setAccessible(true);
            return (Map<String, Deque<Instant>>) f.get(limiter);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void login_is_allowed_initially() {
        LoginRateLimitInMemory limiter = new LoginRateLimitInMemory();

        assertDoesNotThrow(() -> limiter.checkLoginAllowed("user@example.com"));
    }

    @Test
    void login_is_blocked_after_five_failed_attempts() {
        LoginRateLimitInMemory limiter = new LoginRateLimitInMemory();
        String key = "user@example.com";

        for (int i = 0; i < 5; i++) {
            limiter.recordFailedLogin(key);
        }

        RateLimitException exception = assertThrows(
                RateLimitException.class,
                () -> limiter.checkLoginAllowed(key)
        );

        assertEquals("Too many failed login attempts", exception.getMessage());
    }

    @Test
    void login_failures_are_tracked_per_key() {
        LoginRateLimitInMemory limiter = new LoginRateLimitInMemory();

        for (int i = 0; i < 5; i++) {
            limiter.recordFailedLogin("blocked@example.com");
        }

        assertThrows(
                RateLimitException.class,
                () -> limiter.checkLoginAllowed("blocked@example.com")
        );

        assertDoesNotThrow(() -> limiter.checkLoginAllowed("other@example.com"));
    }

    @Test
    void reset_login_failures_removes_lock() {
        LoginRateLimitInMemory limiter = new LoginRateLimitInMemory();
        String key = "user@example.com";

        for (int i = 0; i < 5; i++) {
            limiter.recordFailedLogin(key);
        }

        assertThrows(RateLimitException.class, () -> limiter.checkLoginAllowed(key));

        limiter.resetLoginFailures(key);

        assertDoesNotThrow(() -> limiter.checkLoginAllowed(key));
    }

    @Test
    void reset_login_failures_requires_five_new_failures_to_lock_again() {
        LoginRateLimitInMemory limiter = new LoginRateLimitInMemory();
        String key = "user@example.com";

        for (int i = 0; i < 5; i++) {
            limiter.recordFailedLogin(key);
        }
        assertThrows(RateLimitException.class, () -> limiter.checkLoginAllowed(key));

        limiter.resetLoginFailures(key);

        for (int i = 0; i < 4; i++) {
            limiter.recordFailedLogin(key);
        }
        assertDoesNotThrow(() -> limiter.checkLoginAllowed(key));

        limiter.recordFailedLogin(key);
        assertThrows(RateLimitException.class, () -> limiter.checkLoginAllowed(key));
    }

    @Test
    void reset_login_failures_is_safe_for_unknown_key() {
        LoginRateLimitInMemory limiter = new LoginRateLimitInMemory();

        assertDoesNotThrow(() -> limiter.resetLoginFailures("unknown@example.com"));
        assertDoesNotThrow(() -> limiter.checkLoginAllowed("unknown@example.com"));
    }

    @Test
    void password_reset_is_allowed_initially() {
        LoginRateLimitInMemory limiter = new LoginRateLimitInMemory();

        assertDoesNotThrow(() -> limiter.checkPasswordResetAllowed("user@example.com"));
    }

    @Test
    void password_reset_is_blocked_after_three_requests() {
        LoginRateLimitInMemory limiter = new LoginRateLimitInMemory();
        String key = "user@example.com";

        limiter.recordPasswordResetRequest(key);
        limiter.recordPasswordResetRequest(key);
        limiter.recordPasswordResetRequest(key);

        RateLimitException exception = assertThrows(
                RateLimitException.class,
                () -> limiter.checkPasswordResetAllowed(key)
        );

        assertEquals("Too many password reset requests", exception.getMessage());
    }

    @Test
    void password_reset_requests_are_tracked_per_key() {
        LoginRateLimitInMemory limiter = new LoginRateLimitInMemory();

        limiter.recordPasswordResetRequest("blocked@example.com");
        limiter.recordPasswordResetRequest("blocked@example.com");
        limiter.recordPasswordResetRequest("blocked@example.com");

        assertThrows(
                RateLimitException.class,
                () -> limiter.checkPasswordResetAllowed("blocked@example.com")
        );

        assertDoesNotThrow(() -> limiter.checkPasswordResetAllowed("other@example.com"));
    }

    @Test
    void password_reset_limit_is_independent_from_login_limit() {
        LoginRateLimitInMemory limiter = new LoginRateLimitInMemory();
        String key = "user@example.com";

        for (int i = 0; i < 5; i++) {
            limiter.recordFailedLogin(key);
        }

        assertThrows(RateLimitException.class, () -> limiter.checkLoginAllowed(key));
        assertDoesNotThrow(() -> limiter.checkPasswordResetAllowed(key));
    }

    @Test
    void login_limit_is_independent_from_password_reset_limit() {
        LoginRateLimitInMemory limiter = new LoginRateLimitInMemory();
        String key = "user@example.com";

        limiter.recordPasswordResetRequest(key);
        limiter.recordPasswordResetRequest(key);
        limiter.recordPasswordResetRequest(key);

        assertThrows(RateLimitException.class, () -> limiter.checkPasswordResetAllowed(key));
        assertDoesNotThrow(() -> limiter.checkLoginAllowed(key));
    }

    @Test
    void reset_login_failures_does_not_reset_password_reset_limit() {
        LoginRateLimitInMemory limiter = new LoginRateLimitInMemory();
        String key = "user@example.com";

        limiter.recordPasswordResetRequest(key);
        limiter.recordPasswordResetRequest(key);
        limiter.recordPasswordResetRequest(key);

        limiter.resetLoginFailures(key);

        assertThrows(RateLimitException.class, () -> limiter.checkPasswordResetAllowed(key));
    }

    @Test
    void password_reset_check_is_safe_for_unknown_key() {
        LoginRateLimitInMemory limiter = new LoginRateLimitInMemory();

        assertDoesNotThrow(() -> limiter.checkPasswordResetAllowed("unknown@example.com"));
    }

    @Test
    void login_check_blocks_before_expiry_and_allows_after_expiry() throws Exception {
        LoginRateLimitInMemory limiter = new LoginRateLimitInMemory();
        String key = "user@example.com";

        lockedUntil(limiter).put(key, Instant.now().plusMillis(80));

        assertThrows(RateLimitException.class, () -> limiter.checkLoginAllowed(key));

        Thread.sleep(120);

        assertDoesNotThrow(() -> limiter.checkLoginAllowed(key));
    }

    @Test
    void old_failed_logins_outside_window_do_not_count_towards_lock() {
        LoginRateLimitInMemory limiter = new LoginRateLimitInMemory();
        String key = "user@example.com";

        Deque<Instant> seeded = new ArrayDeque<>();
        Instant now = Instant.now();
        for (int i = 0; i < 4; i++) {
            seeded.addLast(now.minus(Duration.ofMinutes(16)));
        }
        failedLogins(limiter).put(key, seeded);

        limiter.recordFailedLogin(key);

        assertDoesNotThrow(() -> limiter.checkLoginAllowed(key));
    }

    @Test
    void password_reset_requests_older_than_window_are_purged_before_limit_check() {
        LoginRateLimitInMemory limiter = new LoginRateLimitInMemory();
        String key = "user@example.com";

        Deque<Instant> seeded = new ArrayDeque<>();
        Instant now = Instant.now();
        for (int i = 0; i < 3; i++) {
            seeded.addLast(now.minus(Duration.ofHours(1)).minusSeconds(1));
        }
        passwordResetRequests(limiter).put(key, seeded);

        assertDoesNotThrow(() -> limiter.checkPasswordResetAllowed(key));
    }

    @Test
    void password_reset_requests_inside_window_still_enforce_limit() {
        LoginRateLimitInMemory limiter = new LoginRateLimitInMemory();
        String key = "user@example.com";

        Deque<Instant> seeded = new ArrayDeque<>();
        Instant now = Instant.now();
        for (int i = 0; i < 3; i++) {
            seeded.addLast(now.minus(Duration.ofMinutes(59)));
        }
        passwordResetRequests(limiter).put(key, seeded);

        assertThrows(RateLimitException.class, () -> limiter.checkPasswordResetAllowed(key));
    }

    @Test
    void reset_of_one_key_does_not_affect_other_key_lock_state() {
        LoginRateLimitInMemory limiter = new LoginRateLimitInMemory();

        for (int i = 0; i < 5; i++) {
            limiter.recordFailedLogin("a@example.com");
            limiter.recordFailedLogin("b@example.com");
        }

        limiter.resetLoginFailures("a@example.com");

        assertDoesNotThrow(() -> limiter.checkLoginAllowed("a@example.com"));
        assertThrows(RateLimitException.class, () -> limiter.checkLoginAllowed("b@example.com"));
    }
}
