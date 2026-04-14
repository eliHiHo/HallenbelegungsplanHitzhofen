package de.hallenbelegung.application.domain.service;

import de.hallenbelegung.application.domain.exception.ForbiddenException;
import de.hallenbelegung.application.domain.exception.NotFoundException;
import de.hallenbelegung.application.domain.exception.ValidationException;
import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.out.PasswordHashingPort;
import de.hallenbelegung.application.domain.port.out.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class UserServiceTest {

    private UserRepositoryPort userRepository;
    private PasswordHashingPort passwordHashingPort;

    private UserService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepositoryPort.class);
        passwordHashingPort = mock(PasswordHashingPort.class);

        service = new UserService(userRepository, passwordHashingPort);
    }

    private User createAdmin() {
        Instant now = Instant.now();
        return new User(
                UUID.randomUUID(),
                "Admin",
                "User",
                "admin@example.com",
                "hash",
                Role.ADMIN,
                true,
                now,
                now
        );
    }

    private User createInactiveAdmin() {
        Instant now = Instant.now();
        return new User(
                UUID.randomUUID(),
                "Inactive",
                "Admin",
                "inactive-admin@example.com",
                "hash",
                Role.ADMIN,
                false,
                now,
                now
        );
    }

    private User createRepresentative() {
        Instant now = Instant.now();
        return new User(
                UUID.randomUUID(),
                "Club",
                "Rep",
                "rep@example.com",
                "hash",
                Role.CLUB_REPRESENTATIVE,
                true,
                now,
                now
        );
    }

    private User createOtherRepresentative() {
        Instant now = Instant.now();
        return new User(
                UUID.randomUUID(),
                "Other",
                "Rep",
                "other@example.com",
                "hash",
                Role.CLUB_REPRESENTATIVE,
                true,
                now,
                now
        );
    }

    @Test
    void getAllUsers_allows_admin_and_sorts_by_createdAt_desc() {
        User admin = createAdmin();

        User older = new User(
                UUID.randomUUID(),
                "Older",
                "User",
                "older@example.com",
                "hash",
                Role.CLUB_REPRESENTATIVE,
                true,
                Instant.parse("2026-04-01T10:00:00Z"),
                Instant.parse("2026-04-01T10:00:00Z")
        );

        User newer = new User(
                UUID.randomUUID(),
                "Newer",
                "User",
                "newer@example.com",
                "hash",
                Role.CLUB_REPRESENTATIVE,
                true,
                Instant.parse("2026-04-05T10:00:00Z"),
                Instant.parse("2026-04-05T10:00:00Z")
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findAll()).thenReturn(List.of(older, newer));

        List<User> result = service.getAllUsers(admin.getId());

        assertEquals(2, result.size());
        assertEquals(newer.getId(), result.get(0).getId());
        assertEquals(older.getId(), result.get(1).getId());
    }

    @Test
    void getAllUsers_rejects_unknown_user() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.getAllUsers(userId)
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void getAllUsers_rejects_non_admin() {
        User representative = createRepresentative();

        when(userRepository.findById(representative.getId())).thenReturn(Optional.of(representative));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.getAllUsers(representative.getId())
        );

        assertEquals("User not allowed to manage users", exception.getMessage());
    }

    @Test
    void getAllUsers_rejects_inactive_admin() {
        User inactiveAdmin = createInactiveAdmin();

        when(userRepository.findById(inactiveAdmin.getId())).thenReturn(Optional.of(inactiveAdmin));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.getAllUsers(inactiveAdmin.getId())
        );

        assertEquals("User inactive", exception.getMessage());
    }

    @Test
    void getUserById_allows_admin() {
        User admin = createAdmin();
        User target = createRepresentative();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        User result = service.getUserById(admin.getId(), target.getId());

        assertEquals(target.getId(), result.getId());
        assertEquals(target.getEmail(), result.getEmail());
    }

    @Test
    void getUserById_rejects_unknown_admin() {
        UUID adminId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        when(userRepository.findById(adminId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.getUserById(adminId, targetId)
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void getUserById_rejects_non_admin() {
        User representative = createRepresentative();
        User target = createOtherRepresentative();

        when(userRepository.findById(representative.getId())).thenReturn(Optional.of(representative));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.getUserById(representative.getId(), target.getId())
        );

        assertEquals("User not allowed to manage users", exception.getMessage());
    }

    @Test
    void getUserById_rejects_inactive_admin() {
        User inactiveAdmin = createInactiveAdmin();
        User target = createRepresentative();

        when(userRepository.findById(inactiveAdmin.getId())).thenReturn(Optional.of(inactiveAdmin));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.getUserById(inactiveAdmin.getId(), target.getId())
        );

        assertEquals("User inactive", exception.getMessage());
    }

    @Test
    void getUserById_rejects_unknown_target_user() {
        User admin = createAdmin();
        UUID targetId = UUID.randomUUID();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(targetId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.getUserById(admin.getId(), targetId)
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void createUser_success_for_admin() {
        User admin = createAdmin();
        UUID savedId = UUID.randomUUID();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordHashingPort.hash("secret123")).thenReturn("hashed:secret123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            return new User(
                    savedId,
                    u.getFirstName(),
                    u.getLastName(),
                    u.getEmail(),
                    u.getPasswordHash(),
                    u.getRole(),
                    u.isActive(),
                    u.getCreatedAt(),
                    u.getUpdatedAt()
            );
        });

        UUID result = service.createUser(
                admin.getId(),
                "New",
                "User",
                "new@example.com",
                "secret123",
                Role.CLUB_REPRESENTATIVE
        );

        assertEquals(savedId, result);
        verify(passwordHashingPort).hash("secret123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_normalizes_email_to_lowercase() {
        User admin = createAdmin();
        UUID savedId = UUID.randomUUID();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordHashingPort.hash("secret123")).thenReturn("hashed:secret123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            return new User(
                    savedId,
                    u.getFirstName(),
                    u.getLastName(),
                    u.getEmail(),
                    u.getPasswordHash(),
                    u.getRole(),
                    u.isActive(),
                    u.getCreatedAt(),
                    u.getUpdatedAt()
            );
        });

        UUID result = service.createUser(
                admin.getId(),
                "New",
                "User",
                "  NEW@EXAMPLE.COM  ",
                "secret123",
                Role.CLUB_REPRESENTATIVE
        );

        assertEquals(savedId, result);
        verify(userRepository).findByEmail("new@example.com");
    }

    @Test
    void createUser_rejects_unknown_admin() {
        UUID adminId = UUID.randomUUID();

        when(userRepository.findById(adminId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.createUser(
                        adminId,
                        "New",
                        "User",
                        "new@example.com",
                        "secret123",
                        Role.CLUB_REPRESENTATIVE
                )
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void createUser_rejects_non_admin() {
        User representative = createRepresentative();

        when(userRepository.findById(representative.getId())).thenReturn(Optional.of(representative));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.createUser(
                        representative.getId(),
                        "New",
                        "User",
                        "new@example.com",
                        "secret123",
                        Role.CLUB_REPRESENTATIVE
                )
        );

        assertEquals("User not allowed to manage users", exception.getMessage());
    }

    @Test
    void createUser_rejects_inactive_admin() {
        User inactiveAdmin = createInactiveAdmin();

        when(userRepository.findById(inactiveAdmin.getId())).thenReturn(Optional.of(inactiveAdmin));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.createUser(
                        inactiveAdmin.getId(),
                        "New",
                        "User",
                        "new@example.com",
                        "secret123",
                        Role.CLUB_REPRESENTATIVE
                )
        );

        assertEquals("User inactive", exception.getMessage());
    }

    @Test
    void createUser_rejects_blank_first_name() {
        User admin = createAdmin();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.createUser(
                        admin.getId(),
                        "   ",
                        "User",
                        "new@example.com",
                        "secret123",
                        Role.CLUB_REPRESENTATIVE
                )
        );

        assertEquals("First name is required", exception.getMessage());
    }

    @Test
    void createUser_rejects_blank_last_name() {
        User admin = createAdmin();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.createUser(
                        admin.getId(),
                        "New",
                        "   ",
                        "new@example.com",
                        "secret123",
                        Role.CLUB_REPRESENTATIVE
                )
        );

        assertEquals("Last name is required", exception.getMessage());
    }

    @Test
    void createUser_rejects_too_long_first_name() {
        User admin = createAdmin();
        String tooLong = "a".repeat(101);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.createUser(
                        admin.getId(),
                        tooLong,
                        "User",
                        "new@example.com",
                        "secret123",
                        Role.CLUB_REPRESENTATIVE
                )
        );

        assertEquals("First name is too long", exception.getMessage());
    }

    @Test
    void createUser_rejects_too_long_last_name() {
        User admin = createAdmin();
        String tooLong = "a".repeat(101);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.createUser(
                        admin.getId(),
                        "New",
                        tooLong,
                        "new@example.com",
                        "secret123",
                        Role.CLUB_REPRESENTATIVE
                )
        );

        assertEquals("Last name is too long", exception.getMessage());
    }

    @Test
    void createUser_rejects_blank_email() {
        User admin = createAdmin();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.createUser(
                        admin.getId(),
                        "New",
                        "User",
                        "   ",
                        "secret123",
                        Role.CLUB_REPRESENTATIVE
                )
        );

        assertEquals("Email is required", exception.getMessage());
    }

    @Test
    void createUser_rejects_invalid_email_without_at() {
        User admin = createAdmin();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.createUser(
                        admin.getId(),
                        "New",
                        "User",
                        "not-an-email",
                        "secret123",
                        Role.CLUB_REPRESENTATIVE
                )
        );

        assertEquals("Email is invalid", exception.getMessage());
    }

    @Test
    void createUser_rejects_invalid_email_starting_with_at() {
        User admin = createAdmin();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.createUser(
                        admin.getId(),
                        "New",
                        "User",
                        "@example.com",
                        "secret123",
                        Role.CLUB_REPRESENTATIVE
                )
        );

        assertEquals("Email is invalid", exception.getMessage());
    }

    @Test
    void createUser_rejects_invalid_email_ending_with_at() {
        User admin = createAdmin();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.createUser(
                        admin.getId(),
                        "New",
                        "User",
                        "test@",
                        "secret123",
                        Role.CLUB_REPRESENTATIVE
                )
        );

        assertEquals("Email is invalid", exception.getMessage());
    }

    @Test
    void createUser_rejects_blank_password() {
        User admin = createAdmin();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.createUser(
                        admin.getId(),
                        "New",
                        "User",
                        "new@example.com",
                        "   ",
                        Role.CLUB_REPRESENTATIVE
                )
        );

        assertEquals("Password is required", exception.getMessage());
    }

    @Test
    void createUser_rejects_short_password() {
        User admin = createAdmin();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.createUser(
                        admin.getId(),
                        "New",
                        "User",
                        "new@example.com",
                        "short",
                        Role.CLUB_REPRESENTATIVE
                )
        );

        assertEquals("Password must have at least 8 characters", exception.getMessage());
    }

    @Test
    void createUser_rejects_null_role() {
        User admin = createAdmin();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.createUser(
                        admin.getId(),
                        "New",
                        "User",
                        "new@example.com",
                        "secret123",
                        null
                )
        );

        assertEquals("Role is required", exception.getMessage());
    }

    @Test
    void createUser_rejects_duplicate_email() {
        User admin = createAdmin();
        User existing = createRepresentative();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findByEmail("rep@example.com")).thenReturn(Optional.of(existing));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.createUser(
                        admin.getId(),
                        "New",
                        "User",
                        "rep@example.com",
                        "secret123",
                        Role.CLUB_REPRESENTATIVE
                )
        );

        assertEquals("Email already in use", exception.getMessage());
    }

    @Test
    void updateUser_success_for_admin_changes_profile_role_and_active() {
        User admin = createAdmin();
        User target = createRepresentative();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(userRepository.findByEmail("updated@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateUser(
                admin.getId(),
                target.getId(),
                "Updated",
                "Name",
                "updated@example.com",
                Role.ADMIN,
                false
        );

        assertEquals("Updated", target.getFirstName());
        assertEquals("Name", target.getLastName());
        assertEquals("updated@example.com", target.getEmail());
        assertEquals(Role.ADMIN, target.getRole());
        assertFalse(target.isActive());

        verify(userRepository).save(target);
    }

    @Test
    void updateUser_allows_null_active_and_keeps_state() {
        User admin = createAdmin();
        User target = createRepresentative();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(userRepository.findByEmail("updated@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateUser(
                admin.getId(),
                target.getId(),
                "Updated",
                "Name",
                "updated@example.com",
                Role.CLUB_REPRESENTATIVE,
                null
        );

        assertTrue(target.isActive());
        assertEquals(Role.CLUB_REPRESENTATIVE, target.getRole());
    }

    @Test
    void updateUser_reactivates_user_when_active_true() {
        User admin = createAdmin();
        User target = new User(
                UUID.randomUUID(),
                "Club",
                "Rep",
                "rep@example.com",
                "hash",
                Role.CLUB_REPRESENTATIVE,
                false,
                Instant.now(),
                Instant.now()
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(userRepository.findByEmail("rep@example.com")).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateUser(
                admin.getId(),
                target.getId(),
                "Club",
                "Rep",
                "rep@example.com",
                Role.CLUB_REPRESENTATIVE,
                true
        );

        assertTrue(target.isActive());
    }

    @Test
    void updateUser_rejects_unknown_admin() {
        UUID adminId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        when(userRepository.findById(adminId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.updateUser(
                        adminId,
                        targetId,
                        "Updated",
                        "Name",
                        "updated@example.com",
                        Role.ADMIN,
                        true
                )
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void updateUser_rejects_non_admin() {
        User representative = createRepresentative();
        User target = createOtherRepresentative();

        when(userRepository.findById(representative.getId())).thenReturn(Optional.of(representative));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.updateUser(
                        representative.getId(),
                        target.getId(),
                        "Updated",
                        "Name",
                        "updated@example.com",
                        Role.ADMIN,
                        true
                )
        );

        assertEquals("User not allowed to manage users", exception.getMessage());
    }

    @Test
    void updateUser_rejects_inactive_admin() {
        User inactiveAdmin = createInactiveAdmin();
        User target = createRepresentative();

        when(userRepository.findById(inactiveAdmin.getId())).thenReturn(Optional.of(inactiveAdmin));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.updateUser(
                        inactiveAdmin.getId(),
                        target.getId(),
                        "Updated",
                        "Name",
                        "updated@example.com",
                        Role.ADMIN,
                        true
                )
        );

        assertEquals("User inactive", exception.getMessage());
    }

    @Test
    void updateUser_rejects_unknown_target_user() {
        User admin = createAdmin();
        UUID targetId = UUID.randomUUID();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(targetId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.updateUser(
                        admin.getId(),
                        targetId,
                        "Updated",
                        "Name",
                        "updated@example.com",
                        Role.ADMIN,
                        true
                )
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void updateUser_rejects_blank_first_name() {
        User admin = createAdmin();
        User target = createRepresentative();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.updateUser(
                        admin.getId(),
                        target.getId(),
                        "   ",
                        "Name",
                        "updated@example.com",
                        Role.ADMIN,
                        true
                )
        );

        assertEquals("First name is required", exception.getMessage());
    }

    @Test
    void updateUser_rejects_blank_last_name() {
        User admin = createAdmin();
        User target = createRepresentative();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.updateUser(
                        admin.getId(),
                        target.getId(),
                        "Updated",
                        "   ",
                        "updated@example.com",
                        Role.ADMIN,
                        true
                )
        );

        assertEquals("Last name is required", exception.getMessage());
    }

    @Test
    void updateUser_rejects_too_long_first_name() {
        User admin = createAdmin();
        User target = createRepresentative();
        String tooLong = "a".repeat(101);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.updateUser(
                        admin.getId(),
                        target.getId(),
                        tooLong,
                        "Name",
                        "updated@example.com",
                        Role.ADMIN,
                        true
                )
        );

        assertEquals("First name is too long", exception.getMessage());
    }

    @Test
    void updateUser_rejects_too_long_last_name() {
        User admin = createAdmin();
        User target = createRepresentative();
        String tooLong = "a".repeat(101);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.updateUser(
                        admin.getId(),
                        target.getId(),
                        "Updated",
                        tooLong,
                        "updated@example.com",
                        Role.ADMIN,
                        true
                )
        );

        assertEquals("Last name is too long", exception.getMessage());
    }

    @Test
    void updateUser_rejects_blank_email() {
        User admin = createAdmin();
        User target = createRepresentative();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.updateUser(
                        admin.getId(),
                        target.getId(),
                        "Updated",
                        "Name",
                        "   ",
                        Role.ADMIN,
                        true
                )
        );

        assertEquals("Email is required", exception.getMessage());
    }

    @Test
    void updateUser_rejects_invalid_email() {
        User admin = createAdmin();
        User target = createRepresentative();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.updateUser(
                        admin.getId(),
                        target.getId(),
                        "Updated",
                        "Name",
                        "invalid-email",
                        Role.ADMIN,
                        true
                )
        );

        assertEquals("Email is invalid", exception.getMessage());
    }

    @Test
    void updateUser_rejects_null_role() {
        User admin = createAdmin();
        User target = createRepresentative();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.updateUser(
                        admin.getId(),
                        target.getId(),
                        "Updated",
                        "Name",
                        "updated@example.com",
                        null,
                        true
                )
        );

        assertEquals("Role is required", exception.getMessage());
    }

    @Test
    void updateUser_rejects_duplicate_email_of_other_user() {
        User admin = createAdmin();
        User target = createRepresentative();
        User otherUser = createOtherRepresentative();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(userRepository.findByEmail(otherUser.getEmail())).thenReturn(Optional.of(otherUser));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.updateUser(
                        admin.getId(),
                        target.getId(),
                        "Updated",
                        "Name",
                        otherUser.getEmail(),
                        Role.ADMIN,
                        true
                )
        );

        assertEquals("Email already in use", exception.getMessage());
    }

    @Test
    void updateUser_allows_same_email_for_same_user() {
        User admin = createAdmin();
        User target = createRepresentative();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(userRepository.findByEmail(target.getEmail())).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> service.updateUser(
                admin.getId(),
                target.getId(),
                "Updated",
                "Name",
                target.getEmail(),
                Role.CLUB_REPRESENTATIVE,
                true
        ));

        assertEquals("Updated", target.getFirstName());
        assertEquals(target.getEmail(), target.getEmail());
    }

    @Test
    void updateUser_rejects_admin_deactivating_own_account() {
        User admin = createAdmin();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.updateUser(
                        admin.getId(),
                        admin.getId(),
                        admin.getFirstName(),
                        admin.getLastName(),
                        admin.getEmail(),
                        admin.getRole(),
                        false
                )
        );

        assertEquals("Admin cannot deactivate own account", exception.getMessage());
    }
}