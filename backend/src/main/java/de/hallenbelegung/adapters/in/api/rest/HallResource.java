package de.hallenbelegung.adapters.in.api.rest;

import de.hallenbelegung.adapters.in.api.dto.HallDTO;
import de.hallenbelegung.adapters.in.api.mapper.HallApiMapper;
import de.hallenbelegung.application.domain.exception.UnauthorizedException;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.in.GetCurrentUserUseCase;
import de.hallenbelegung.application.domain.port.in.GetHallUseCase;
import de.hallenbelegung.application.domain.port.in.ManageHallUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
@Path("/halls")
@Produces(MediaType.APPLICATION_JSON)
public class HallResource {

    private static final String SESSION_COOKIE_NAME = "HB_SESSION";

    private final GetHallUseCase getHallUseCase;
    private final ManageHallUseCase manageHallUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;

    public HallResource(
            GetHallUseCase getHallUseCase,
            ManageHallUseCase manageHallUseCase,
            GetCurrentUserUseCase getCurrentUserUseCase
    ) {
        this.getHallUseCase = getHallUseCase;
        this.manageHallUseCase = manageHallUseCase;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
    }

    @GET
    public List<HallDTO> getAll(
            @QueryParam("includeInactive") Boolean includeInactive,
            @CookieParam(SESSION_COOKIE_NAME) String sessionId
    ) {
        boolean include = Boolean.TRUE.equals(includeInactive);

        if (include) {
            User currentUser = getCurrentUserUseCase.getCurrentUser(requireSessionId(sessionId));
            return manageHallUseCase.getAllIncludingInactive(currentUser.getId())
                    .stream()
                    .map(HallApiMapper::toDTO)
                    .toList();
        }

        return getHallUseCase.getAllActive()
                .stream()
                .map(HallApiMapper::toDTO)
                .toList();
    }

    @GET
    @Path("/{hallId}")
    public HallDTO getById(
            @PathParam("hallId") UUID hallId,
            @QueryParam("includeInactive") Boolean includeInactive,
            @CookieParam(SESSION_COOKIE_NAME) String sessionId
    ) {
        boolean include = Boolean.TRUE.equals(includeInactive);

        if (include) {
            User currentUser = getCurrentUserUseCase.getCurrentUser(requireSessionId(sessionId));
            return HallApiMapper.toDTO(
                    manageHallUseCase.getByIdIncludingInactive(currentUser.getId(), hallId)
            );
        }

        return HallApiMapper.toDTO(getHallUseCase.getById(hallId));
    }

    private String requireSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new UnauthorizedException("Missing session cookie");
        }
        return sessionId;
    }
}