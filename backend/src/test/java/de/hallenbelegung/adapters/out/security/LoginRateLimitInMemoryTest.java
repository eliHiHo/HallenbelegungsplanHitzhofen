package de.hallenbelegung.adapters.out.security;

import de.hallenbelegung.application.domain.exception.RateLimitException;
import de.hallenbelegung.application.domain.port.out.LoginRateLimitPort;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LoginRateLimitInMemoryTest {

    @Test
    public void testFailedLoginLockingAndReset() {
        LoginRateLimitInMemory limiter = new LoginRateLimitInMemory();
        String key = "user@example.com";

        // ensure fresh
        limiter.resetLoginFailures(key);

        // record MAX_FAILED_ATTEMPTS (5) times
        for (int i = 0; i < 5; i++) {
            limiter.recordFailedLogin(key);
        }

        // now checkLoginAllowed should throw RateLimitException
        assertThrows(RateLimitException.class, () -> limiter.checkLoginAllowed(key));

        // reset and then allowed
        limiter.resetLoginFailures(key);
        assertDoesNotThrow(() -> limiter.checkLoginAllowed(key));
    }
}
