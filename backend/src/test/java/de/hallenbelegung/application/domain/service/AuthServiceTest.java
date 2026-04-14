package de.hallenbelegung.application.domain.service;

import de.hallenbelegung.application.domain.exception.ForbiddenException;
import de.hallenbelegung.application.domain.exception.NotFoundException;
import de.hallenbelegung.application.domain.exception.ValidationException;
import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.out.LoginRateLimitPort;
import de.hallenbelegung.application.domain.port.out.PasswordHashingPort;
import de.hallenbelegung.application.domain.port.out.PasswordResetMailPort;
import de.hallenbelegung.application.domain.port.out.PasswordResetPort;
import de.hallenbelegung.application.domain.port.out.PasswordVerificationPort;
import de.hallenbelegung.application.domain.port.out.SessionPort;
import de.hallenbelegung.application.domain.port.out.UserRepositoryPort;
import de.hallenbelegung.application.domain.view.AuthSessionView;
import de.hallenbelegung.application.domain.view.SessionUserView;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AuthServiceTest {

    static class InMemoryUserRepo implements UserRepositoryPort {
        private final Map<UUID, User> usersById = new HashMap<>();
        private final Map<String, UUID> userIdByEmail = new HashMap<>();

        @Override
        public User save(User user) {
            User toStore = user;

            if (toStore.getId() == null) {
                Instant now = Instant.now();
                toStore = new User(
                        UUID.randomUUID(),
                        toStore.getFirstName(),
                        toStore.getLastName(),
                        toStore.getEmail(),
                        toStore.getPasswordHash(),
                        toStore.getRole(),
                        toStore.isActive(),
                        now,
                        now
                );
            }

            usersById.put(toStore.getId(), toStore);
            userIdByEmail.put(toStore.getEmail(), toStore.getId());
            return toStore;
        }

        @Override
        public Optional<User> findById(UUID userId) {
            return Optional.ofNullable(usersById.get(userId));
        }

        @Override
        public Optional<User> findByEmail(String email) {
            UUID userId = userIdByEmail.get(email);
            if (userId == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(usersById.get(userId));
        }

        @Override
        public List<User> findAll() {
            return List.copyOf(usersById.values());
        }

        @Override
        public List<User> findAllActive() {
            return usersById.values().stream().filter(User::isActive).toList();
        }
    }

    static class ConfigurablePasswordVerifier implements PasswordVerificationPort {
        boolean nextMatchResult;
        String lastRaw;
        String lastHash;

        ConfigurablePasswordVerifier(boolean nextMatchResult) {
            this.nextMatchResult = nextMatchResult;
        }

        @Override
        public boolean matches(String raw, String hash) {
            this.lastRaw = raw;
            this.lastHash = hash;
            return nextMatchResult;
        }
    }

    static class SimpleHash implements PasswordHashingPort {
        @Override
        public String hash(String raw) {
            return "hashed:" + raw;
        }
    }

    static class CapturingSessionPort implements SessionPort {
        String createdForEmail;
        Duration createdTimeout;
        String returnedToken = "session-token";

        String invalidatedSessionId;
        UUID invalidatedSessionsForUserId;
        String touchedSessionId;
        Duration touchedTimeout;

        final Map<String, SessionUserView> sessions = new HashMap<>();

        @Override
        public String createSession(User user, Duration inactivityTimeout) {
            createdForEmail = user.getEmail();
            createdTimeout = inactivityTimeout;
            sessions.put(returnedToken, new SessionUserView(returnedToken, user.getId()));
            return returnedToken;
        }

        @Override
        public Optional<SessionUserView> findActiveSession(String sessionId) {
            return Optional.ofNullable(sessions.get(sessionId));
        }

        @Override
        public void invalidateSession(String sessionId) {
            invalidatedSessionId = sessionId;
            sessions.remove(sessionId);
        }

        @Override
        public void invalidateSessionsByUserId(UUID userId) {
            invalidatedSessionsForUserId = userId;
            sessions.entrySet().removeIf(entry -> entry.getValue().getUserId().equals(userId));
        }

        @Override
        public void touchSession(String sessionId, Duration inactivityTimeout) {
            touchedSessionId = sessionId;
            touchedTimeout = inactivityTimeout;
        }
    }

    static class CapturingPasswordResetPort implements PasswordResetPort {
        UUID createdForUserId;
        Duration createdValidity;
        String createdToken = "reset-token";

        String invalidatedToken;
        UUID invalidatedTokensForUserId;

        final Map<String, UUID> validTokens = new HashMap<>();

        @Override
        public String createToken(UUID userId, Duration validity) {
            createdForUserId = userId;
            createdValidity = validity;
            validTokens.put(createdToken, userId);
            return createdToken;
        }

        @Override
        public Optional<UUID> findUserIdByValidToken(String token) {
            return Optional.ofNullable(validTokens.get(token));
        }

        @Override
        public void invalidateToken(String token) {
            invalidatedToken = token;
            validTokens.remove(token);
        }

        @Override
        public void invalidateTokensByUserId(UUID userId) {
            invalidatedTokensForUserId = userId;
            validTokens.entrySet().removeIf(entry -> entry.getValue().equals(userId));
        }
    }

    static class CapturingMailPort implements PasswordResetMailPort {
        boolean sent;
        User sentToUser;
        String sentToken;

        @Override
        public void sendPasswordResetMail(User user, String resetTokenOrLink) {
            sent = true;
            sentToUser = user;
            sentToken = resetTokenOrLink;
        }
    }

    static class CapturingLoginRateLimitPort implements LoginRateLimitPort {
        String checkedLoginKey;
        String failedLoginKey;
        String resetLoginKey;

        String checkedPasswordResetKey;
        String recordedPasswordResetKey;

        @Override
        public void checkLoginAllowed(String key) {
            checkedLoginKey = key;
        }

        @Override
        public void recordFailedLogin(String key) {
            failedLoginKey = key;
        }

        @Override
        public void resetLoginFailures(String key) {
            resetLoginKey = key;
        }

        @Override
        public void checkPasswordResetAllowed(String key) {
            checkedPasswordResetKey = key;
        }

        @Override
        public void recordPasswordResetRequest(String key) {
            recordedPasswordResetKey = key;
        }
    }

    static class ThrowingLoginRateLimitPort extends CapturingLoginRateLimitPort {
        RuntimeException loginException;
        RuntimeException passwordResetException;

        @Override
        public void checkLoginAllowed(String key) {
            super.checkLoginAllowed(key);
            if (loginException != null) {
                throw loginException;
            }
        }

        @Override
        public void checkPasswordResetAllowed(String key) {
            super.checkPasswordResetAllowed(key);
            if (passwordResetException != null) {
                throw passwordResetException;
            }
        }
    }

    static class CapturingHash implements PasswordHashingPort {
        String lastRaw;

        @Override
        public String hash(String raw) {
            lastRaw = raw;
            return "hashed:" + raw;
        }
    }

    private static User createAndSaveActiveUser(InMemoryUserRepo userRepo, String email) {
        return userRepo.save(User.createNew(
                "John",
                "Doe",
                email,
                "stored-hash",
                Role.ADMIN
        ));
    }

    private static User createAndSaveInactiveUser(InMemoryUserRepo userRepo, String email) {
        User user = userRepo.save(User.createNew(
                "Jane",
                "Doe",
                email,
                "stored-hash",
                Role.CLUB_REPRESENTATIVE
        ));
        user.deactivate();
        userRepo.save(user);
        return user;
    }

    private static AuthService createService(InMemoryUserRepo userRepo,
                                             ConfigurablePasswordVerifier verifier,
                                             PasswordHashingPort hasher,
                                             CapturingSessionPort sessionPort,
                                             CapturingPasswordResetPort resetPort,
                                             CapturingMailPort mailPort,
                                             CapturingLoginRateLimitPort rateLimitPort) {
        return new AuthService(
                userRepo,
                verifier,
                hasher,
                sessionPort,
                resetPort,
                mailPort,
                rateLimitPort
        );
    }

    @Test
    void login_success_for_active_user() {
        InMemoryUserRepo userRepo = new InMemoryUserRepo();
        User user = createAndSaveActiveUser(userRepo, "john@example.com");
        ConfigurablePasswordVerifier verifier = new ConfigurablePasswordVerifier(true);
        CapturingSessionPort sessionPort = new CapturingSessionPort();
        CapturingLoginRateLimitPort rateLimitPort = new CapturingLoginRateLimitPort();

        AuthService service = createService(
                userRepo,
                verifier,
                new SimpleHash(),
                sessionPort,
                new CapturingPasswordResetPort(),
                new CapturingMailPort(),
                rateLimitPort
        );

        AuthSessionView result = service.login("john@example.com", "secret123");

        assertNotNull(result);
        assertEquals("session-token", result.getSessionId());
        assertEquals(user.getId(), result.getUserId());
        assertEquals(user.getEmail(), result.getEmail());
        assertEquals(user.getRole(), result.getRole());
        assertEquals("john@example.com", rateLimitPort.checkedLoginKey);
        assertEquals("john@example.com", rateLimitPort.resetLoginKey);
        assertEquals(user.getId(), sessionPort.invalidatedSessionsForUserId);
        assertEquals("john@example.com", sessionPort.createdForEmail);
        assertEquals(Duration.ofMinutes(30), sessionPort.createdTimeout);
        assertEquals("secret123", verifier.lastRaw);
        assertEquals("stored-hash", verifier.lastHash);
    }

    @Test
    void login_rejects_unknown_email() {
        InMemoryUserRepo userRepo = new InMemoryUserRepo();
        CapturingLoginRateLimitPort rateLimitPort = new CapturingLoginRateLimitPort();

        AuthService service = createService(
                userRepo,
                new ConfigurablePasswordVerifier(true),
                new SimpleHash(),
                new CapturingSessionPort(),
                new CapturingPasswordResetPort(),
                new CapturingMailPort(),
                rateLimitPort
        );

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.login("unknown@example.com", "secret123")
        );

        assertEquals("Invalid credentials", exception.getMessage());
        assertEquals("unknown@example.com", rateLimitPort.checkedLoginKey);
        assertEquals("unknown@example.com", rateLimitPort.failedLoginKey);
        assertNull(rateLimitPort.resetLoginKey);
    }

    @Test
    void login_stops_immediately_when_rate_limited() {
        InMemoryUserRepo userRepo = new InMemoryUserRepo();
        createAndSaveActiveUser(userRepo, "john@example.com");

        ConfigurablePasswordVerifier verifier = new ConfigurablePasswordVerifier(true);
        CapturingSessionPort sessionPort = new CapturingSessionPort();
        ThrowingLoginRateLimitPort rateLimitPort = new ThrowingLoginRateLimitPort();
        rateLimitPort.loginException = new ForbiddenException("Too many login attempts");

        AuthService service = createService(
                userRepo,
                verifier,
                new SimpleHash(),
                sessionPort,
                new CapturingPasswordResetPort(),
                new CapturingMailPort(),
                rateLimitPort
        );

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.login("john@example.com", "secret123")
        );

        assertEquals("Too many login attempts", exception.getMessage());
        assertEquals("john@example.com", rateLimitPort.checkedLoginKey);
        assertNull(rateLimitPort.failedLoginKey);
        assertNull(rateLimitPort.resetLoginKey);
        assertNull(verifier.lastRaw);
        assertNull(sessionPort.invalidatedSessionsForUserId);
        assertNull(sessionPort.createdForEmail);
    }

    @Test
    void login_rejects_wrong_password() {
        InMemoryUserRepo userRepo = new InMemoryUserRepo();
        createAndSaveActiveUser(userRepo, "john@example.com");
        CapturingLoginRateLimitPort rateLimitPort = new CapturingLoginRateLimitPort();

        AuthService service = createService(
                userRepo,
                new ConfigurablePasswordVerifier(false),
                new SimpleHash(),
                new CapturingSessionPort(),
                new CapturingPasswordResetPort(),
                new CapturingMailPort(),
                rateLimitPort
        );

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.login("john@example.com", "wrong-password")
        );

        assertEquals("Invalid credentials", exception.getMessage());
        assertEquals("john@example.com", rateLimitPort.failedLoginKey);
        assertNull(rateLimitPort.resetLoginKey);
    }

    @Test
    void login_rejects_inactive_user() {
        InMemoryUserRepo userRepo = new InMemoryUserRepo();
        createAndSaveInactiveUser(userRepo, "inactive@example.com");

        CapturingSessionPort sessionPort = new CapturingSessionPort();
        CapturingLoginRateLimitPort rateLimitPort = new CapturingLoginRateLimitPort();

        AuthService service = createService(
                userRepo,
                new ConfigurablePasswordVerifier(true),
                new SimpleHash(),
                sessionPort,
                new CapturingPasswordResetPort(),
                new CapturingMailPort(),
                rateLimitPort
        );

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.login("inactive@example.com", "secret123")
        );

        assertEquals("User account is inactive", exception.getMessage());
        assertNull(rateLimitPort.failedLoginKey);
        assertNull(rateLimitPort.resetLoginKey);
        assertNull(sessionPort.invalidatedSessionsForUserId);
        assertNull(sessionPort.createdForEmail);
    }

    @Test
    void login_normalizes_email_to_lowercase() {
        InMemoryUserRepo userRepo = new InMemoryUserRepo();
        User user = createAndSaveActiveUser(userRepo, "john@example.com");
        CapturingLoginRateLimitPort rateLimitPort = new CapturingLoginRateLimitPort();
        CapturingSessionPort sessionPort = new CapturingSessionPort();

        AuthService service = createService(
                userRepo,
                new ConfigurablePasswordVerifier(true),
                new SimpleHash(),
                sessionPort,
                new CapturingPasswordResetPort(),
                new CapturingMailPort(),
                rateLimitPort
        );

        AuthSessionView result = service.login("  JOHN@EXAMPLE.COM  ", "secret123");

        assertEquals(user.getId(), result.getUserId());
        assertEquals("john@example.com", rateLimitPort.checkedLoginKey);
        assertEquals("john@example.com", rateLimitPort.resetLoginKey);
        assertEquals("john@example.com", sessionPort.createdForEmail);
    }

    @Test
    void login_resets_rate_limit_after_success() {
        InMemoryUserRepo userRepo = new InMemoryUserRepo();
        createAndSaveActiveUser(userRepo, "john@example.com");
        CapturingLoginRateLimitPort rateLimitPort = new CapturingLoginRateLimitPort();

        AuthService service = createService(
                userRepo,
                new ConfigurablePasswordVerifier(true),
                new SimpleHash(),
                new CapturingSessionPort(),
                new CapturingPasswordResetPort(),
                new CapturingMailPort(),
                rateLimitPort
        );

        service.login("john@example.com", "secret123");

        assertEquals("john@example.com", rateLimitPort.resetLoginKey);
        assertNull(rateLimitPort.failedLoginKey);
    }

    @Test
    void logout_invalidates_session() {
        AuthService service = createService(
                new InMemoryUserRepo(),
                new ConfigurablePasswordVerifier(true),
                new SimpleHash(),
                new CapturingSessionPort(),
                new CapturingPasswordResetPort(),
                new CapturingMailPort(),
                new CapturingLoginRateLimitPort()
        );

        CapturingSessionPort sessionPort = new CapturingSessionPort();
        service = createService(
                new InMemoryUserRepo(),
                new ConfigurablePasswordVerifier(true),
                new SimpleHash(),
                sessionPort,
                new CapturingPasswordResetPort(),
                new CapturingMailPort(),
                new CapturingLoginRateLimitPort()
        );

        service.logout("session-123");

        assertEquals("session-123", sessionPort.invalidatedSessionId);
    }

    @Test
    void getCurrentUser_returns_user_for_valid_session() {
        InMemoryUserRepo userRepo = new InMemoryUserRepo();
        User user = createAndSaveActiveUser(userRepo, "john@example.com");
        CapturingSessionPort sessionPort = new CapturingSessionPort();
        sessionPort.sessions.put("session-123", new SessionUserView("session-123", user.getId()));

        AuthService service = createService(
                userRepo,
                new ConfigurablePasswordVerifier(true),
                new SimpleHash(),
                sessionPort,
                new CapturingPasswordResetPort(),
                new CapturingMailPort(),
                new CapturingLoginRateLimitPort()
        );

        User currentUser = service.getCurrentUser("session-123");

        assertEquals(user.getId(), currentUser.getId());
        assertEquals("session-123", sessionPort.touchedSessionId);
        assertEquals(Duration.ofMinutes(30), sessionPort.touchedTimeout);
    }

    @Test
    void getCurrentUser_rejects_missing_session() {
        AuthService service = createService(
                new InMemoryUserRepo(),
                new ConfigurablePasswordVerifier(true),
                new SimpleHash(),
                new CapturingSessionPort(),
                new CapturingPasswordResetPort(),
                new CapturingMailPort(),
                new CapturingLoginRateLimitPort()
        );

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.getCurrentUser("   ")
        );

        assertEquals("Session id is required", exception.getMessage());
    }

    @Test
    void getCurrentUser_rejects_expired_session() {
        AuthService service = createService(
                new InMemoryUserRepo(),
                new ConfigurablePasswordVerifier(true),
                new SimpleHash(),
                new CapturingSessionPort(),
                new CapturingPasswordResetPort(),
                new CapturingMailPort(),
                new CapturingLoginRateLimitPort()
        );

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.getCurrentUser("expired-session")
        );

        assertEquals("Session invalid or expired", exception.getMessage());
    }

    @Test
    void getCurrentUser_invalidates_session_if_user_inactive() {
        InMemoryUserRepo userRepo = new InMemoryUserRepo();
        User user = createAndSaveInactiveUser(userRepo, "inactive@example.com");

        CapturingSessionPort sessionPort = new CapturingSessionPort();
        sessionPort.sessions.put("session-123", new SessionUserView("session-123", user.getId()));

        AuthService service = createService(
                userRepo,
                new ConfigurablePasswordVerifier(true),
                new SimpleHash(),
                sessionPort,
                new CapturingPasswordResetPort(),
                new CapturingMailPort(),
                new CapturingLoginRateLimitPort()
        );

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.getCurrentUser("session-123")
        );

        assertEquals("User account is inactive", exception.getMessage());
        assertEquals("session-123", sessionPort.invalidatedSessionId);
        assertNull(sessionPort.touchedSessionId);
    }

    @Test
    void forgotPassword_does_not_fail_for_unknown_email() {
        CapturingLoginRateLimitPort rateLimitPort = new CapturingLoginRateLimitPort();
        CapturingMailPort mailPort = new CapturingMailPort();
        CapturingPasswordResetPort resetPort = new CapturingPasswordResetPort();

        AuthService service = createService(
                new InMemoryUserRepo(),
                new ConfigurablePasswordVerifier(true),
                new SimpleHash(),
                new CapturingSessionPort(),
                resetPort,
                mailPort,
                rateLimitPort
        );

        assertDoesNotThrow(() -> service.requestPasswordReset("unknown@example.com"));

        assertEquals("unknown@example.com", rateLimitPort.checkedPasswordResetKey);
        assertEquals("unknown@example.com", rateLimitPort.recordedPasswordResetKey);
        assertFalse(mailPort.sent);
        assertNull(resetPort.createdForUserId);
    }

    @Test
    void forgotPassword_skips_inactive_user() {
        InMemoryUserRepo userRepo = new InMemoryUserRepo();
        User user = createAndSaveInactiveUser(userRepo, "inactive@example.com");

        CapturingPasswordResetPort resetPort = new CapturingPasswordResetPort();
        CapturingMailPort mailPort = new CapturingMailPort();

        AuthService service = createService(
                userRepo,
                new ConfigurablePasswordVerifier(true),
                new SimpleHash(),
                new CapturingSessionPort(),
                resetPort,
                mailPort,
                new CapturingLoginRateLimitPort()
        );

        assertDoesNotThrow(() -> service.requestPasswordReset("inactive@example.com"));

        assertFalse(mailPort.sent);
        assertNull(resetPort.createdForUserId);
        assertNull(resetPort.invalidatedTokensForUserId);
        assertNotNull(userRepo.findById(user.getId()).orElse(null));
    }

    @Test
    void forgotPassword_creates_token_and_sends_mail_for_active_user() {
        InMemoryUserRepo userRepo = new InMemoryUserRepo();
        User user = createAndSaveActiveUser(userRepo, "john@example.com");

        CapturingPasswordResetPort resetPort = new CapturingPasswordResetPort();
        CapturingMailPort mailPort = new CapturingMailPort();
        CapturingLoginRateLimitPort rateLimitPort = new CapturingLoginRateLimitPort();

        AuthService service = createService(
                userRepo,
                new ConfigurablePasswordVerifier(true),
                new SimpleHash(),
                new CapturingSessionPort(),
                resetPort,
                mailPort,
                rateLimitPort
        );

        service.requestPasswordReset("john@example.com");

        assertEquals("john@example.com", rateLimitPort.checkedPasswordResetKey);
        assertEquals("john@example.com", rateLimitPort.recordedPasswordResetKey);
        assertEquals(user.getId(), resetPort.invalidatedTokensForUserId);
        assertEquals(user.getId(), resetPort.createdForUserId);
        assertEquals(Duration.ofHours(2), resetPort.createdValidity);
        assertTrue(mailPort.sent);
        assertEquals(user.getId(), mailPort.sentToUser.getId());
        assertEquals("reset-token", mailPort.sentToken);
    }

    @Test
    void forgotPassword_stops_when_rate_limited() {
        InMemoryUserRepo userRepo = new InMemoryUserRepo();
        User user = createAndSaveActiveUser(userRepo, "john@example.com");

        CapturingPasswordResetPort resetPort = new CapturingPasswordResetPort();
        CapturingMailPort mailPort = new CapturingMailPort();
        ThrowingLoginRateLimitPort rateLimitPort = new ThrowingLoginRateLimitPort();
        rateLimitPort.passwordResetException = new ForbiddenException("Too many reset requests");

        AuthService service = createService(
                userRepo,
                new ConfigurablePasswordVerifier(true),
                new SimpleHash(),
                new CapturingSessionPort(),
                resetPort,
                mailPort,
                rateLimitPort
        );

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.requestPasswordReset("john@example.com")
        );

        assertEquals("Too many reset requests", exception.getMessage());
        assertEquals("john@example.com", rateLimitPort.checkedPasswordResetKey);
        assertNull(rateLimitPort.recordedPasswordResetKey);
        assertFalse(mailPort.sent);
        assertNull(resetPort.invalidatedTokensForUserId);
        assertNull(resetPort.createdForUserId);
        assertNotNull(userRepo.findById(user.getId()).orElse(null));
    }

    @Test
    void resetPassword_rejects_blank_token() {
        AuthService service = createService(
                new InMemoryUserRepo(),
                new ConfigurablePasswordVerifier(true),
                new SimpleHash(),
                new CapturingSessionPort(),
                new CapturingPasswordResetPort(),
                new CapturingMailPort(),
                new CapturingLoginRateLimitPort()
        );

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.resetPassword("   ", "newpassword")
        );

        assertEquals("Reset token is required", exception.getMessage());
    }

    @Test
    void resetPassword_rejects_short_password() {
        InMemoryUserRepo userRepo = new InMemoryUserRepo();
        User user = createAndSaveActiveUser(userRepo, "john@example.com");
        CapturingPasswordResetPort resetPort = new CapturingPasswordResetPort();
        resetPort.validTokens.put("reset-token", user.getId());
        CapturingSessionPort sessionPort = new CapturingSessionPort();

        AuthService service = createService(
                userRepo,
                new ConfigurablePasswordVerifier(true),
                new SimpleHash(),
                sessionPort,
                resetPort,
                new CapturingMailPort(),
                new CapturingLoginRateLimitPort()
        );

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.resetPassword("reset-token", "short")
        );

        assertEquals("Password must have at least 8 characters", exception.getMessage());
        User unchangedUser = userRepo.findById(user.getId()).orElseThrow();
        assertEquals("stored-hash", unchangedUser.getPasswordHash());
        assertNull(resetPort.invalidatedToken);
        assertNull(sessionPort.invalidatedSessionsForUserId);
    }

    @Test
    void resetPassword_rejects_invalid_token() {
        AuthService service = createService(
                new InMemoryUserRepo(),
                new ConfigurablePasswordVerifier(true),
                new SimpleHash(),
                new CapturingSessionPort(),
                new CapturingPasswordResetPort(),
                new CapturingMailPort(),
                new CapturingLoginRateLimitPort()
        );

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.resetPassword("invalid-token", "newpassword")
        );

        assertEquals("Reset token invalid or expired", exception.getMessage());
    }

    @Test
    void resetPassword_rejects_inactive_user() {
        InMemoryUserRepo userRepo = new InMemoryUserRepo();
        User user = createAndSaveInactiveUser(userRepo, "inactive@example.com");

        CapturingPasswordResetPort resetPort = new CapturingPasswordResetPort();
        resetPort.validTokens.put("reset-token", user.getId());
        CapturingSessionPort sessionPort = new CapturingSessionPort();
        CapturingHash hasher = new CapturingHash();

        AuthService service = createService(
                userRepo,
                new ConfigurablePasswordVerifier(true),
                hasher,
                sessionPort,
                resetPort,
                new CapturingMailPort(),
                new CapturingLoginRateLimitPort()
        );

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.resetPassword("reset-token", "newpassword")
        );

        assertEquals("User account is inactive", exception.getMessage());
        assertNull(hasher.lastRaw);
        assertNull(resetPort.invalidatedToken);
        assertNull(sessionPort.invalidatedSessionsForUserId);
    }

    @Test
    void resetPassword_rejects_when_token_user_is_missing_without_side_effects() {
        InMemoryUserRepo userRepo = new InMemoryUserRepo();

        CapturingPasswordResetPort resetPort = new CapturingPasswordResetPort();
        resetPort.validTokens.put("reset-token", UUID.randomUUID());
        CapturingSessionPort sessionPort = new CapturingSessionPort();
        CapturingHash hasher = new CapturingHash();

        AuthService service = createService(
                userRepo,
                new ConfigurablePasswordVerifier(true),
                hasher,
                sessionPort,
                resetPort,
                new CapturingMailPort(),
                new CapturingLoginRateLimitPort()
        );

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.resetPassword("reset-token", "newpassword")
        );

        assertEquals("User not found", exception.getMessage());
        assertNull(hasher.lastRaw);
        assertNull(resetPort.invalidatedToken);
        assertNull(sessionPort.invalidatedSessionsForUserId);
    }

    @Test
    void resetPassword_updates_hash_invalidates_token_and_sessions() {
        InMemoryUserRepo userRepo = new InMemoryUserRepo();
        User user = createAndSaveActiveUser(userRepo, "john@example.com");

        CapturingPasswordResetPort resetPort = new CapturingPasswordResetPort();
        resetPort.validTokens.put("reset-token", user.getId());

        CapturingSessionPort sessionPort = new CapturingSessionPort();

        AuthService service = createService(
                userRepo,
                new ConfigurablePasswordVerifier(true),
                new SimpleHash(),
                sessionPort,
                resetPort,
                new CapturingMailPort(),
                new CapturingLoginRateLimitPort()
        );

        service.resetPassword("reset-token", "newpassword");

        User updatedUser = userRepo.findById(user.getId()).orElseThrow();
        assertEquals("hashed:newpassword", updatedUser.getPasswordHash());
        assertEquals("reset-token", resetPort.invalidatedToken);
        assertEquals(user.getId(), sessionPort.invalidatedSessionsForUserId);
    }
}