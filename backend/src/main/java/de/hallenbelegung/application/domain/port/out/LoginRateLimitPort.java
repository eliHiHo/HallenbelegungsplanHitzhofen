package de.hallenbelegung.application.domain.port.out;

public interface LoginRateLimitPort {

    // LOGIN

    void checkLoginAllowed(String key);

    void recordFailedLogin(String key);

    void resetLoginFailures(String key);

    // PASSWORD RESET

    void checkPasswordResetAllowed(String key);

    void recordPasswordResetRequest(String key);
}