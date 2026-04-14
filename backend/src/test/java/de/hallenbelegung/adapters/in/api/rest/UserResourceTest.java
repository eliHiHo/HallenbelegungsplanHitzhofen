package de.hallenbelegung.adapters.in.api.rest;

import de.hallenbelegung.adapters.in.api.dto.CreateUserDTO;
import de.hallenbelegung.adapters.in.api.dto.EmptyResponseDTO;
import de.hallenbelegung.adapters.in.api.dto.UserDTO;
import de.hallenbelegung.application.domain.exception.UnauthorizedException;
import de.hallenbelegung.application.domain.exception.ValidationException;
import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.in.CreateUserUseCase;
import de.hallenbelegung.application.domain.port.in.GetCurrentUserUseCase;
import de.hallenbelegung.application.domain.port.in.GetUserByIdUseCase;
import de.hallenbelegung.application.domain.port.in.GetUsersUseCase;
import de.hallenbelegung.application.domain.port.in.UpdateUserUseCase;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserResourceTest {

    private GetCurrentUserUseCase getCurrentUserUseCase;
    private GetUsersUseCase getUsersUseCase;
    private GetUserByIdUseCase getUserByIdUseCase;
    private CreateUserUseCase createUserUseCase;
    private UpdateUserUseCase updateUserUseCase;

    private UserResource resource;

    @BeforeEach
    void setUp() {
        getCurrentUserUseCase = mock(GetCurrentUserUseCase.class);
        getUsersUseCase = mock(GetUsersUseCase.class);
        getUserByIdUseCase = mock(GetUserByIdUseCase.class);
        createUserUseCase = mock(CreateUserUseCase.class);
        updateUserUseCase = mock(UpdateUserUseCase.class);

        resource = new UserResource(
                getCurrentUserUseCase,
                getUsersUseCase,
                getUserByIdUseCase,
                createUserUseCase,
                updateUserUseCase
        );
    }

    private User user(UUID id, Role role, boolean active) {
        return new User(id, "Max", "Mustermann", "max@example.com", "hash", role, active, Instant.now(), Instant.now());
    }

    @Test
    void getAll_requires_cookie_and_maps() {
        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> resource.getAll(" "));
        assertEquals("Missing session cookie", ex.getMessage());

        UUID adminId = UUID.randomUUID();
        User admin = user(adminId, Role.ADMIN, true);
        User target = user(UUID.randomUUID(), Role.CLUB_REPRESENTATIVE, true);
        when(getCurrentUserUseCase.getCurrentUser("sess")).thenReturn(admin);
        when(getUsersUseCase.getAllUsers(adminId)).thenReturn(List.of(target));

        List<UserDTO> result = resource.getAll("sess");

        assertEquals(1, result.size());
        assertEquals(target.getId(), result.get(0).id());
        assertEquals("CLUB_REPRESENTATIVE", result.get(0).role());
    }

    @Test
    void getById_maps_user() {
        UUID adminId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        User admin = user(adminId, Role.ADMIN, true);
        User target = user(targetId, Role.CLUB_REPRESENTATIVE, false);

        when(getCurrentUserUseCase.getCurrentUser("sess")).thenReturn(admin);
        when(getUserByIdUseCase.getUserById(adminId, targetId)).thenReturn(target);

        UserDTO dto = resource.getById(targetId, "sess");

        assertEquals(targetId, dto.id());
        assertEquals(false, dto.active());
    }

    @Test
    void createUser_validates_required_fields_and_returns_location() {
        UUID adminId = UUID.randomUUID();
        User admin = user(adminId, Role.ADMIN, true);
        when(getCurrentUserUseCase.getCurrentUser("sess")).thenReturn(admin);

        ValidationException ex1 = assertThrows(ValidationException.class, () -> resource.createUser(null, "sess"));
        assertEquals("Request body is required", ex1.getMessage());

        ValidationException ex2 = assertThrows(
                ValidationException.class,
                () -> resource.createUser(new CreateUserDTO("A", "B", " ", "pw", "ADMIN"), "sess")
        );
        assertEquals("Email is required", ex2.getMessage());

        ValidationException ex3 = assertThrows(
                ValidationException.class,
                () -> resource.createUser(new CreateUserDTO("A", "B", "a@b.c", " ", "ADMIN"), "sess")
        );
        assertEquals("Password is required", ex3.getMessage());

        UUID createdId = UUID.randomUUID();
        when(createUserUseCase.createUser(adminId, "A", "B", "a@b.c", "pw", Role.ADMIN)).thenReturn(createdId);

        Response response = resource.createUser(new CreateUserDTO("A", "B", "a@b.c", "pw", "ADMIN"), "sess");

        assertEquals(201, response.getStatus());
        assertInstanceOf(EmptyResponseDTO.class, response.getEntity());
        assertEquals("/users/" + createdId, response.getHeaderString("Location"));
    }

    @Test
    void createUser_rejects_invalid_role_string() {
        UUID adminId = UUID.randomUUID();
        when(getCurrentUserUseCase.getCurrentUser("sess")).thenReturn(user(adminId, Role.ADMIN, true));

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> resource.createUser(new CreateUserDTO("A", "B", "a@b.c", "pw", "INVALID"), "sess")
        );
        assertEquals("Invalid role: INVALID. Allowed values: [ADMIN, CLUB_REPRESENTATIVE]", ex.getMessage());
    }

    @Test
    void updateUser_validates_request_and_role_handling() {
        UUID adminId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        when(getCurrentUserUseCase.getCurrentUser("sess")).thenReturn(user(adminId, Role.ADMIN, true));

        ValidationException ex1 = assertThrows(ValidationException.class, () -> resource.updateUser(targetId, null, "sess"));
        assertEquals("Request body is required", ex1.getMessage());

        ValidationException ex2 = assertThrows(
                ValidationException.class,
                () -> resource.updateUser(targetId, new UserDTO(null, "A", "B", "A B", "a@b.c", " ", true), "sess")
        );
        assertEquals("role must not be blank", ex2.getMessage());

        Response response = resource.updateUser(
                targetId,
                new UserDTO(null, "A", "B", "A B", "a@b.c", "CLUB_REPRESENTATIVE", true),
                "sess"
        );

        assertEquals(200, response.getStatus());
        assertInstanceOf(EmptyResponseDTO.class, response.getEntity());
        verify(updateUserUseCase).updateUser(adminId, targetId, "A", "B", "a@b.c", Role.CLUB_REPRESENTATIVE, true);
    }
}

