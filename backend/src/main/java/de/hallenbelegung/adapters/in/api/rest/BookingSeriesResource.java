package de.hallenbelegung.adapters.in.api.rest;

import de.hallenbelegung.adapters.in.api.dto.BookingSeriesDTO;
import de.hallenbelegung.adapters.in.api.dto.EmptyResponseDTO;
import de.hallenbelegung.adapters.in.api.mapper.BookingSeriesApiMapper;
import de.hallenbelegung.application.domain.exception.UnauthorizedException;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.in.CancelBookingSeriesUseCase;
import de.hallenbelegung.application.domain.port.in.GetBookingSeriesUseCase;
import de.hallenbelegung.application.domain.port.in.GetCurrentUserUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@ApplicationScoped
@Path("/booking-series")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class BookingSeriesResource {

    private static final String SESSION_COOKIE_NAME = "HB_SESSION";

    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final GetBookingSeriesUseCase getBookingSeriesUseCase;
    private final CancelBookingSeriesUseCase cancelBookingSeriesUseCase;

    public BookingSeriesResource(
            GetCurrentUserUseCase getCurrentUserUseCase,
            GetBookingSeriesUseCase getBookingSeriesUseCase,
            CancelBookingSeriesUseCase cancelBookingSeriesUseCase
    ) {
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.getBookingSeriesUseCase = getBookingSeriesUseCase;
        this.cancelBookingSeriesUseCase = cancelBookingSeriesUseCase;
    }

    @GET
    @Path("/{id}")
    public BookingSeriesDTO getById(
            @PathParam("id") UUID id,
            @CookieParam(SESSION_COOKIE_NAME) String sessionId
    ) {
        User currentUser = getCurrentUserUseCase.getCurrentUser(requireSessionId(sessionId));
        return BookingSeriesApiMapper.toDTO(
                getBookingSeriesUseCase.getById(currentUser.getId(), id)
        );
    }

    /**
     * Cancels an entire booking series including all future occurrences.
     * The optional cancellation reason is passed as a query parameter.
     */
    @DELETE
    @Path("/{id}")
    public Response cancelSeries(
            @PathParam("id") UUID id,
            @QueryParam("reason") String reason,
            @CookieParam(SESSION_COOKIE_NAME) String sessionId
    ) {
        User currentUser = getCurrentUserUseCase.getCurrentUser(requireSessionId(sessionId));
        cancelBookingSeriesUseCase.cancelSeries(currentUser.getId(), id, reason);
        return Response.ok(new EmptyResponseDTO()).build();
    }

    private String requireSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new UnauthorizedException("Missing session cookie");
        }
        return sessionId;
    }
}
