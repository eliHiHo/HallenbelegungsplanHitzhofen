package de.hallenbelegung.adapters.in.api.rest;

import de.hallenbelegung.adapters.in.api.dto.BookingDTO;
import de.hallenbelegung.adapters.in.api.dto.BookingRequestDTO;
import de.hallenbelegung.adapters.in.api.dto.BookingFeedbackDTO;
import de.hallenbelegung.adapters.in.api.dto.EmptyResponseDTO;
import de.hallenbelegung.adapters.in.api.mapper.BookingApiMapper;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.in.CancelBookingUseCase;
import de.hallenbelegung.application.domain.port.in.GetBookingUseCase;
import de.hallenbelegung.application.domain.port.in.GetCurrentUserUseCase;
import de.hallenbelegung.application.domain.port.in.UpdateBookingFeedbackUseCase;
import de.hallenbelegung.application.domain.port.in.UpdateBookingUseCase;
import de.hallenbelegung.application.domain.view.BookingDetailView;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@ApplicationScoped
@Path("/bookings")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class BookingResource {

    private static final String SESSION_COOKIE_NAME = "HB_SESSION";

    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final GetBookingUseCase getBookingUseCase;
    private final UpdateBookingUseCase updateBookingUseCase;
    private final CancelBookingUseCase cancelBookingUseCase;
    private final UpdateBookingFeedbackUseCase updateBookingFeedbackUseCase;

    public BookingResource(
            GetCurrentUserUseCase getCurrentUserUseCase,
            GetBookingUseCase getBookingUseCase,
            UpdateBookingUseCase updateBookingUseCase,
            CancelBookingUseCase cancelBookingUseCase,
            UpdateBookingFeedbackUseCase updateBookingFeedbackUseCase
    ) {
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.getBookingUseCase = getBookingUseCase;
        this.updateBookingUseCase = updateBookingUseCase;
        this.cancelBookingUseCase = cancelBookingUseCase;
        this.updateBookingFeedbackUseCase = updateBookingFeedbackUseCase;
    }

    @GET
    @Path("/{id}")
    public BookingDTO getById(
            @PathParam("id") UUID id,
            @CookieParam(SESSION_COOKIE_NAME) String sessionId
    ) {
        UUID currentUserId = resolveCurrentUserId(sessionId);
        BookingDetailView view = getBookingUseCase.getById(currentUserId, id);
        return BookingApiMapper.toDTO(view);
    }

    /**
     * Updates a booking (admin only – service enforces role check).
     * Input uses BookingRequestDTO fields: hallId, title, description, startDateTime, endDateTime.
     * Returns the updated booking as BookingDTO by fetching it after the update.
     */
    @PUT
    @Path("/{id}")
    public BookingDTO update(
            @PathParam("id") UUID id,
            BookingRequestDTO request,
            @CookieParam(SESSION_COOKIE_NAME) String sessionId
    ) {
        User currentUser = getCurrentUserUseCase.getCurrentUser(requireSessionId(sessionId));
        updateBookingUseCase.update(
                currentUser.getId(),
                id,
                request.hallId(),
                request.title(),
                request.description(),
                request.startDateTime(),
                request.endDateTime()
        );
        // Fetch the updated view to return the canonical BookingDTO
        BookingDetailView updated = getBookingUseCase.getById(currentUser.getId(), id);
        return BookingApiMapper.toDTO(updated);
    }

    /**
     * Cancels a booking. The optional cancellation reason is passed as a query parameter.
     */
    @DELETE
    @Path("/{id}")
    public Response cancel(
            @PathParam("id") UUID id,
            @QueryParam("reason") String reason,
            @CookieParam(SESSION_COOKIE_NAME) String sessionId
    ) {
        User currentUser = getCurrentUserUseCase.getCurrentUser(requireSessionId(sessionId));
        cancelBookingUseCase.cancel(currentUser.getId(), id, reason);
        return Response.ok(new EmptyResponseDTO()).build();
    }

    /**
     * Saves feedback (participant count and optional comment) after a booking has been conducted.
     * Body: {"participantCount": 12, "comment": "Gute Stimmung"}
     */
    @PUT
    @Path("/{id}/feedback")
    public Response updateFeedback(
            @PathParam("id") UUID id,
            BookingFeedbackDTO body,
            @CookieParam(SESSION_COOKIE_NAME) String sessionId
    ) {
        User currentUser = getCurrentUserUseCase.getCurrentUser(requireSessionId(sessionId));

        Integer participantCount = body != null ? body.participantCount() : null;
        String comment = body != null ? body.comment() : null;

        updateBookingFeedbackUseCase.updateFeedback(id, participantCount, comment, currentUser.getId());
        return Response.ok(new EmptyResponseDTO()).build();
    }

    private UUID resolveCurrentUserId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }
        User currentUser = getCurrentUserUseCase.getCurrentUser(sessionId);
        return currentUser.getId();
    }

    private String requireSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Missing session cookie");
        }
        return sessionId;
    }
}
