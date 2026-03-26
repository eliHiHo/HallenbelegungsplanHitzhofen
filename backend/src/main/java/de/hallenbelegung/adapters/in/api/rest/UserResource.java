package de.hallenbelegung.adapters.in.api.rest;

import de.hallenbelegung.adapters.in.api.dto.CreateUserDTO;
import de.hallenbelegung.adapters.in.api.dto.EmptyResponseDTO;
import de.hallenbelegung.adapters.in.api.dto.UserDTO;
import de.hallenbelegung.adapters.in.api.mapper.UserApiMapper;
import de.hallenbelegung.application.domain.exception.UnauthorizedException;
import de.hallenbelegung.application.domain.exception.ValidationException;
import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.in.CreateUserUseCase;
import de.hallenbelegung.application.domain.port.in.GetCurrentUserUseCase;
import de.hallenbelegung.application.domain.port.in.GetUserByIdUseCase;
import de.hallenbelegung.application.domain.port.in.GetUsersUseCase;
import de.hallenbelegung.application.domain.port.in.UpdateUserUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {

    private static final String SESSION_COOKIE_NAME = "HB_SESSION";

    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final GetUsersUseCase getUsersUseCase;
    private final GetUserByIdUseCase getUserByIdUseCase;
    private final CreateUserUseCase createUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;

    public UserResource(
            GetCurrentUserUseCase getCurrentUserUseCase,
            GetUsersUseCase getUsersUseCase,
            GetUserByIdUseCase getUserByIdUseCase,
            CreateUserUseCase createUserUseCase,
            UpdateUserUseCase updateUserUseCase
    ) {
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.getUsersUseCase = getUsersUseCase;
        this.getUserByIdUseCase = getUserByIdUseCase;
        this.createUserUseCase = createUserUseCase;
        this.updateUserUseCase = updateUserUseCase;
    }

    @GET
    public List<UserDTO> getAll(
            @CookieParam(SESSION_COOKIE_NAME) String sessionId
    ) {
        User currentUser = getCurrentUserUseCase.getCurrentUser(requireSessionId(sessionId));
        return getUsersUseCase.getAllUsers(currentUser.getId())
                .stream()
                .map(UserApiMapper::toDTO)
                .toList();
    }

    @GET
    @Path("/{id}")
    public UserDTO getById(
            @PathParam("id") UUID id,
            @CookieParam(SESSION_COOKIE_NAME) String sessionId
    ) {
        User currentUser = getCurrentUserUseCase.getCurrentUser(requireSessionId(sessionId));
        return UserApiMapper.toDTO(
                getUserByIdUseCase.getUserById(currentUser.getId(), id)
        );
    }

    /**
     * Creates a new user (admin only – service enforces role check).
     */
    @POST
    public Response createUser(
            CreateUserDTO dto,
            @CookieParam(SESSION_COOKIE_NAME) String sessionId
    ) {
        User currentUser = getCurrentUserUseCase.getCurrentUser(requireSessionId(sessionId));

        if (dto == null) {
            throw new ValidationException("Request body is required");
        }

        if (dto.email() == null || dto.email().isBlank()) {
            throw new ValidationException("Email is required");
        }

        if (dto.password() == null || dto.password().isBlank()) {
            throw new ValidationException("Password is required");
        }

        Role role = parseRole(dto.role());

        UUID created = createUserUseCase.createUser(
                currentUser.getId(),
                dto.firstName(),
                dto.lastName(),
                dto.email(),
                dto.password(),
                role
        );

        return Response.status(Response.Status.CREATED)
                .entity(new EmptyResponseDTO())
                .header("Location", "/users/" + created)
                .build();
    }

    /**
     * Updates an existing user (admin only – service enforces role check).
     * Uses UserDTO as input. The 'id' field in the body is ignored (taken from path).
     * The 'fullName' field in the body is ignored.
     */
    @PUT
    @Path("/{id}")
    public Response updateUser(
            @PathParam("id") UUID id,
            UserDTO request,
            @CookieParam(SESSION_COOKIE_NAME) String sessionId
    ) {
        User currentUser = getCurrentUserUseCase.getCurrentUser(requireSessionId(sessionId));

        if (request == null) {
            throw new ValidationException("Request body is required");
        }

        Role role = request.role() != null ? parseRole(request.role()) : null;

        updateUserUseCase.updateUser(
                currentUser.getId(),
                id,
                request.firstName(),
                request.lastName(),
                request.email(),
                role,
                request.active()
        );

        return Response.ok(new EmptyResponseDTO()).build();
    }

    private Role parseRole(String roleStr) {
        if (roleStr == null) {
            return null; // role is optional
        }
        if (roleStr.isBlank()) {
            throw new ValidationException("role must not be blank");
        }
        try {
            return Role.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            String allowed = String.join(", ", java.util.Arrays.stream(Role.values()).map(Enum::name).toList());
            throw new ValidationException("Invalid role: " + roleStr + ". Allowed values: [" + allowed + "]");
        }
    }

    private String requireSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new UnauthorizedException("Missing session cookie");
        }
        return sessionId;
    }
}