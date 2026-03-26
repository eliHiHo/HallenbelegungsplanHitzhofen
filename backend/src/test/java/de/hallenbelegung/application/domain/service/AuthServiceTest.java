package de.hallenbelegung.application.domain.service;

import de.hallenbelegung.application.domain.exception.ValidationException;
import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.out.*;
import de.hallenbelegung.application.domain.view.AuthSessionView;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AuthServiceTest {

    static class InMemoryUserRepo implements UserRepositoryPort {
        private User user;
        @Override
        public User save(User user) { this.user = user; return user; }
        @Override
        public Optional<User> findById(UUID userId) { return Optional.ofNullable(user).filter(u -> u.getId() != null && u.getId().equals(userId)); }
        @Override
        public Optional<User> findByEmail(String email) { return Optional.ofNullable(user).filter(u -> u.getEmail().equals(email)); }
        @Override public java.util.List<User> findAll() { return java.util.List.of(); }
        @Override public java.util.List<User> findAllActive() { return java.util.List.of(user); }
    }

    static class SimplePasswordVerifier implements PasswordVerificationPort {
        boolean match;
        public SimplePasswordVerifier(boolean match) { this.match = match; }
        @Override public boolean matches(String raw, String hash) { return match; }
    }

    static class SimpleHash implements PasswordHashingPort {
        @Override public String hash(String raw) { return "hashed:" + raw; }
    }

    static class CapturingSessionPort implements SessionPort {
        String createdFor;
        String returnedToken = "session-token";
        @Override public String createSession(User user, Duration inactivityTimeout) { createdFor = user.getEmail(); return returnedToken; }
        @Override public Optional<de.hallenbelegung.application.domain.view.SessionUserView> findActiveSession(String sessionId) { return Optional.empty(); }
        @Override public void invalidateSession(String sessionId) { }
        @Override public void invalidateSessionsByUserId(UUID userId) { }
        @Override public void touchSession(String sessionId, Duration inactivityTimeout) { }
    }

    static class CapturingPasswordResetPort implements PasswordResetPort {
        String lastCreatedToken;
        UUID lastUser;
        @Override public String createToken(UUID userId, Duration validity) { lastUser = userId; lastCreatedToken = "reset-token"; return lastCreatedToken; }
        @Override public Optional<UUID> findUserIdByValidToken(String token) { return Optional.ofNullable(lastUser); }
        @Override public void invalidateToken(String token) { lastCreatedToken = null; }
        @Override public void invalidateTokensByUserId(UUID userId) { }
    }

    static class NoopMail implements PasswordResetMailPort { boolean sent=false; @Override public void sendPasswordResetMail(User user, String resetTokenOrLink) { sent=true; } }

    static class NoopLoginRateLimit implements LoginRateLimitPort {
        @Override public void checkLoginAllowed(String key) {}
        @Override public void recordFailedLogin(String key) {}
        @Override public void resetLoginFailures(String key) {}
        @Override public void checkPasswordResetAllowed(String key) {}
        @Override public void recordPasswordResetRequest(String key) {}
    }

    @Test
    public void loginAndPasswordResetFlow() {
        InMemoryUserRepo userRepo = new InMemoryUserRepo();
        User user = User.createNew("John","Doe","john@example.com","passhash", Role.ADMIN);
        // set id via reflection-like by saving so mapper gives id null -> but domain expects id for login token; we'll save and emulate id by creating new user with id
        user = new User(UUID.randomUUID(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getPasswordHash(), user.getRole(), user.isActive(), user.getCreatedAt(), user.getUpdatedAt());
        userRepo.save(user);

        SimplePasswordVerifier verifier = new SimplePasswordVerifier(true);
        SimpleHash hasher = new SimpleHash();
        CapturingSessionPort sessionPort = new CapturingSessionPort();
        CapturingPasswordResetPort resetPort = new CapturingPasswordResetPort();
        NoopMail mailPort = new NoopMail();
        NoopLoginRateLimit loginLimit = new NoopLoginRateLimit();

        AuthService svc = new AuthService(userRepo, verifier, hasher, sessionPort, resetPort, mailPort, loginLimit);

        AuthSessionView view = svc.login("john@example.com", "password");
        assertNotNull(view);
        assertEquals("session-token", view.getSessionId());
        assertEquals(user.getId(), view.getUserId());

        // request password reset
        svc.requestPasswordReset("john@example.com");
        assertEquals(user.getId(), resetPort.lastUser);
        assertTrue(mailPort.sent);

        // reset password
        svc.resetPassword(resetPort.lastCreatedToken, "newpassword");
        // user saved should have changed password hash
        assertEquals("hashed:newpassword", userRepo.findById(user.getId()).get().getPasswordHash());
    }
}
