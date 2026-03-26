package de.hallenbelegung.adapters.in.api.rest;

import de.hallenbelegung.adapters.in.api.dto.BlockedTimeDTO;
import de.hallenbelegung.adapters.in.api.dto.EmptyResponseDTO;
import de.hallenbelegung.adapters.in.api.mapper.BlockedTimeApiMapper;
import de.hallenbelegung.application.domain.exception.UnauthorizedException;
import de.hallenbelegung.application.domain.exception.ValidationException;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.in.CreateBlockedTimeUseCase;
import de.hallenbelegung.application.domain.port.in.DeleteBlockedTimeUseCase;
import de.hallenbelegung.application.domain.port.in.GetBlockedTimeUseCase;
import de.hallenbelegung.application.domain.port.in.GetCurrentUserUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
@Path("/blocked-times")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class BlockedTimeResource {

    private static final String SESSION_COOKIE_NAME = "HB_SESSION";

    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final GetBlockedTimeUseCase getBlockedTimeUseCase;
    private final CreateBlockedTimeUseCase createBlockedTimeUseCase;
    private final DeleteBlockedTimeUseCase deleteBlockedTimeUseCase;

    public BlockedTimeResource(
            GetCurrentUserUseCase getCurrentUserUseCase,
            GetBlockedTimeUseCase getBlockedTimeUseCase,
            CreateBlockedTimeUseCase createBlockedTimeUseCase,
            DeleteBlockedTimeUseCase deleteBlockedTimeUseCase
    ) {
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.getBlockedTimeUseCase = getBlockedTimeUseCase;
        this.createBlockedTimeUseCase = createBlockedTimeUseCase;
        this.deleteBlockedTimeUseCase = deleteBlockedTimeUseCase;
    }

    @GET
    public List<BlockedTimeDTO> getAll(
            @CookieParam(SESSION_COOKIE_NAME) String sessionId
    ) {
        User currentUser = getCurrentUserUseCase.getCurrentUser(requireSessionId(sessionId));
        return getBlockedTimeUseCase.getAll(currentUser.getId())
                .stream()
                .map(BlockedTimeApiMapper::toDTO)
                .toList();
    }

    /**
     * Creates a new blocked time for the given hall.
     * Reuses BlockedTimeDTO as input: hallId, reason, startDateTime, endDateTime.
     * The id, hallName fields in the body are ignored.
     */
    @POST
    public Response create(
            BlockedTimeDTO request,
            @CookieParam(SESSION_COOKIE_NAME) String sessionId
    ) {
        User currentUser = getCurrentUserUseCase.getCurrentUser(requireSessionId(sessionId));

        if (request == null) {
            throw new ValidationException("Request body is required");
        }
        if (request.hallId() == null) {
            throw new ValidationException("hallId is required");
        }
        if (request.startDateTime() == null) {
            throw new ValidationException("startDateTime is required");
        }
        if (request.endDateTime() == null) {
            throw new ValidationException("endDateTime is required");
        }

        createBlockedTimeUseCase.create(
                request.hallId(),
                request.reason(),
                request.startDateTime(),
                request.endDateTime(),
                currentUser.getId()
        );
        return Response.status(Response.Status.CREATED)
                .entity(new EmptyResponseDTO())
                .build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(
            @PathParam("id") UUID id,
            @CookieParam(SESSION_COOKIE_NAME) String sessionId
    ) {
        User currentUser = getCurrentUserUseCase.getCurrentUser(requireSessionId(sessionId));
        deleteBlockedTimeUseCase.delete(id, currentUser.getId());
        return Response.ok(new EmptyResponseDTO()).build();
    }

    private String requireSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new UnauthorizedException("Missing session cookie");
        }
        return sessionId;
    }
}