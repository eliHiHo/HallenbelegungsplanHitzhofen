package de.hallenbelegung.application.domain.service;

import de.hallenbelegung.application.domain.exception.ForbiddenException;
import de.hallenbelegung.application.domain.exception.NotFoundException;
import de.hallenbelegung.application.domain.exception.ValidationException;
import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.in.CreateUserUseCase;
import de.hallenbelegung.application.domain.port.in.GetUserByIdUseCase;
import de.hallenbelegung.application.domain.port.in.GetUsersUseCase;
import de.hallenbelegung.application.domain.port.in.UpdateUserUseCase;
import de.hallenbelegung.application.domain.port.out.PasswordHashingPort;
import de.hallenbelegung.application.domain.port.out.UserRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
@Transactional
public class UserService implements
        GetUsersUseCase,
        GetUserByIdUseCase,
        CreateUserUseCase,
        UpdateUserUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordHashingPort passwordHashingPort;

    public UserService(UserRepositoryPort userRepository,
                       PasswordHashingPort passwordHashingPort) {
        this.userRepository = userRepository;
        this.passwordHashingPort = passwordHashingPort;
    }

    @Override
    public List<User> getAllUsers(Long adminUserId) {
        User admin = loadActiveAdmin(adminUserId);

        return userRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(User::getCreatedAt).reversed())
                .toList();
    }

    @Override
    public User getUserById(Long adminUserId, Long userId) {
        User admin = loadActiveAdmin(adminUserId);
        return loadUser(userId);
    }

    @Override
    public Long createUser(Long adminUserId,
                           String firstName,
                           String lastName,
                           String email,
                           String rawPassword,
                           Role role) {

        User admin = loadActiveAdmin(adminUserId);

        validateName(firstName, "First name");
        validateName(lastName, "Last name");
        String normalizedEmail = normalizeAndValidateEmail(email);
        validatePassword(rawPassword);
        validateRole(role);

        ensureEmailNotTaken(normalizedEmail, null);

        String passwordHash = passwordHashingPort.hash(rawPassword);

        User user = User.createNew(
                firstName.trim(),
                lastName.trim(),
                normalizedEmail,
                passwordHash,
                role
        );

        User savedUser = userRepository.save(user);
        return savedUser.getId();
    }

    @Override
    public void updateUser(Long adminUserId,
                           Long userId,
                           String firstName,
                           String lastName,
                           String email,
                           Role role,
                           Boolean active) {

        User admin = loadActiveAdmin(adminUserId);
        User user = loadUser(userId);

        validateName(firstName, "First name");
        validateName(lastName, "Last name");
        String normalizedEmail = normalizeAndValidateEmail(email);
        validateRole(role);

        ensureEmailNotTaken(normalizedEmail, user.getId());

        user.updateProfile(
                firstName.trim(),
                lastName.trim(),
                normalizedEmail
        );

        if (user.getRole() != role) {
            user.changeRole(role);
        }

        if (active != null) {
            if (active) {
                user.activate();
            } else {
                if (admin.getId().equals(user.getId())) {
                    throw new ValidationException("Admin cannot deactivate own account");
                }
                user.deactivate();
            }
        }

        userRepository.save(user);
    }

    private User loadActiveAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!user.isActive()) {
            throw new ForbiddenException("User inactive");
        }

        if (!user.isAdmin()) {
            throw new ForbiddenException("User not allowed to manage users");
        }

        return user;
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private void ensureEmailNotTaken(String normalizedEmail, Long currentUserId) {
        userRepository.findByEmail(normalizedEmail).ifPresent(existingUser -> {
            boolean sameUser = currentUserId != null && existingUser.getId().equals(currentUserId);
            if (!sameUser) {
                throw new ValidationException("Email already in use");
            }
        });
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

    private void validateName(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " is required");
        }

        if (value.trim().length() > 100) {
            throw new ValidationException(fieldName + " is too long");
        }
    }

    private void validatePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new ValidationException("Password is required");
        }

        if (rawPassword.length() < 8) {
            throw new ValidationException("Password must have at least 8 characters");
        }
    }

    private void validateRole(Role role) {
        if (role == null) {
            throw new ValidationException("Role is required");
        }
    }
}