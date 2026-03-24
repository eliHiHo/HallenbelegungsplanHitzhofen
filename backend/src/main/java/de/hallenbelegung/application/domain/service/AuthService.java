package de.hallenbelegung.application.domain.service;

import de.hallenbelegung.application.domain.exception.ForbiddenException;
import de.hallenbelegung.application.domain.exception.NotFoundException;
import de.hallenbelegung.application.domain.exception.ValidationException;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.in.*;
import de.hallenbelegung.application.domain.port.out.LoginRateLimitPort;
import de.hallenbelegung.application.domain.port.out.PasswordHashingPort;
import de.hallenbelegung.application.domain.port.out.PasswordResetMailPort;
import de.hallenbelegung.application.domain.port.out.PasswordResetPort;
import de.hallenbelegung.application.domain.port.out.PasswordVerificationPort;
import de.hallenbelegung.application.domain.port.out.SessionPort;
import de.hallenbelegung.application.domain.port.out.UserRepositoryPort;
import de.hallenbelegung.application.domain.view.AuthSessionView;
import de.hallenbelegung.application.domain.view.SessionUserView;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Duration;
import java.util.Locale;

@ApplicationScoped
@Transactional
public class AuthService implements
        LoginUseCase,
        LogoutUseCase,
        GetCurrentUserUseCase,
        ResetPasswordUseCase,
        RequestPasswordResetUseCase {

    private static final Duration SESSION_TIMEOUT = Duration.ofMinutes(30);
    private static final Duration RESET_TOKEN_VALIDITY = Duration.ofHours(2);

    private final UserRepositoryPort userRepository;
    private final PasswordVerificationPort passwordVerificationPort;
    private final PasswordHashingPort passwordHashingPort;
    private final SessionPort sessionPort;
    private final PasswordResetPort passwordResetPort;
    private final PasswordResetMailPort passwordResetMailPort;
    private final LoginRateLimitPort loginRateLimitPort;

    public AuthService(UserRepositoryPort userRepository,
                       PasswordVerificationPort passwordVerificationPort,
                       PasswordHashingPort passwordHashingPort,
                       SessionPort sessionPort,
                       PasswordResetPort passwordResetPort,
                       PasswordResetMailPort passwordResetMailPort,
                       LoginRateLimitPort loginRateLimitPort) {
        this.userRepository = userRepository;
        this.passwordVerificationPort = passwordVerificationPort;
        this.passwordHashingPort = passwordHashingPort;
        this.sessionPort = sessionPort;
        this.passwordResetPort = passwordResetPort;
        this.passwordResetMailPort = passwordResetMailPort;
        this.loginRateLimitPort = loginRateLimitPort;
    }

    @Override
    public AuthSessionView login(String email, String password) {
        String normalizedEmail = normalizeAndValidateEmail(email);
        validatePasswordInput(password);

        loginRateLimitPort.checkLoginAllowed(normalizedEmail);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    loginRateLimitPort.recordFailedLogin(normalizedEmail);
                    return new ValidationException("Invalid credentials");
                });

        if (!user.isActive()) {
            throw new ForbiddenException("User account is inactive");
        }

        boolean passwordMatches = passwordVerificationPort.matches(password, user.getPasswordHash());
        if (!passwordMatches) {
            loginRateLimitPort.recordFailedLogin(normalizedEmail);
            throw new ValidationException("Invalid credentials");
        }

        loginRateLimitPort.resetLoginFailures(normalizedEmail);

        sessionPort.invalidateSessionsByUserId(user.getId());

        String sessionId = sessionPort.createSession(user, SESSION_TIMEOUT);

        return new AuthSessionView(
                sessionId,
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole()
        );
    }

    @Override
    public void logout(String sessionId) {
        validateSessionId(sessionId);
        sessionPort.invalidateSession(sessionId);
    }

    @Override
    public User getCurrentUser(String sessionId) {
        validateSessionId(sessionId);

        SessionUserView sessionUser = sessionPort.findActiveSession(sessionId)
                .orElseThrow(() -> new ForbiddenException("Session invalid or expired"));

        User user = userRepository.findById(sessionUser.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!user.isActive()) {
            sessionPort.invalidateSession(sessionId);
            throw new ForbiddenException("User account is inactive");
        }

        sessionPort.touchSession(sessionId, SESSION_TIMEOUT);
        return user;
    }

    @Override
    public void requestPasswordReset(String email) {
        String normalizedEmail = normalizeAndValidateEmail(email);

        loginRateLimitPort.checkPasswordResetAllowed(normalizedEmail);
        loginRateLimitPort.recordPasswordResetRequest(normalizedEmail);

        userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
            if (!user.isActive()) {
                return;
            }

            passwordResetPort.invalidateTokensByUserId(user.getId());
            String token = passwordResetPort.createToken(user.getId(), RESET_TOKEN_VALIDITY);
            passwordResetMailPort.sendPasswordResetMail(user, token);
        });

        // bewusst keine Exception bei unbekannter Mail
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        validateResetToken(token);
        validateNewPassword(newPassword);

        Long userId = passwordResetPort.findUserIdByValidToken(token)
                .orElseThrow(() -> new ValidationException("Reset token invalid or expired"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!user.isActive()) {
            throw new ForbiddenException("User account is inactive");
        }

        String newPasswordHash = passwordHashingPort.hash(newPassword);
        user.changePasswordHash(newPasswordHash);
        userRepository.save(user);

        passwordResetPort.invalidateToken(token);
        sessionPort.invalidateSessionsByUserId(user.getId());
    }

    private String normalizeAndValidateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ValidationException("Email is required");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        if (!normalizedEmail.contains("@")
                || normalizedEmail.startsWith("@")
                || normalizedEmail.endsWith("@")) {
            throw new ValidationException("Email is invalid");
        }

        return normalizedEmail;
    }

    private void validatePasswordInput(String password) {
        if (password == null || password.isBlank()) {
            throw new ValidationException("Password is required");
        }
    }

    private void validateNewPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new ValidationException("Password is required");
        }

        if (password.length() < 8) {
            throw new ValidationException("Password must have at least 8 characters");
        }
    }

    private void validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new ValidationException("Session id is required");
        }
    }

    private void validateResetToken(String token) {
        if (token == null || token.isBlank()) {
            throw new ValidationException("Reset token is required");
        }
    }
}