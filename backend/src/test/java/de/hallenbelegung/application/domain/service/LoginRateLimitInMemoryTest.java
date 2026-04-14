package de.hallenbelegung.adapters.out.security;

import de.hallenbelegung.application.domain.exception.RateLimitException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LoginRateLimitInMemoryTest {

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
}