package de.hallenbelegung.adapters.in.api.rest;

import de.hallenbelegung.adapters.in.api.dto.BookingRequestDTO;
import de.hallenbelegung.adapters.in.api.dto.EmptyResponseDTO;
import de.hallenbelegung.adapters.in.api.dto.RejectionDTO;
import de.hallenbelegung.adapters.in.api.mapper.BookingRequestApiMapper;
import de.hallenbelegung.application.domain.exception.UnauthorizedException;
import de.hallenbelegung.application.domain.exception.ValidationException;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.in.ApproveBookingRequestUseCase;
import de.hallenbelegung.application.domain.port.in.CreateBookingRequestUseCase;
import de.hallenbelegung.application.domain.port.in.GetBookingRequestUseCase;
import de.hallenbelegung.application.domain.port.in.GetBookingRequestsUseCase;
import de.hallenbelegung.application.domain.port.in.GetCurrentUserUseCase;
import de.hallenbelegung.application.domain.port.in.RejectBookingRequestUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
@Path("/booking-requests")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class BookingRequestResource {

    private static final String SESSION_COOKIE_NAME = "HB_SESSION";

    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final GetBookingRequestsUseCase getBookingRequestsUseCase;
    private final GetBookingRequestUseCase getBookingRequestUseCase;
    private final CreateBookingRequestUseCase createBookingRequestUseCase;
    private final ApproveBookingRequestUseCase approveBookingRequestUseCase;
    private final RejectBookingRequestUseCase rejectBookingRequestUseCase;

    public BookingRequestResource(
            GetCurrentUserUseCase getCurrentUserUseCase,
            GetBookingRequestsUseCase getBookingRequestsUseCase,
            GetBookingRequestUseCase getBookingRequestUseCase,
            CreateBookingRequestUseCase createBookingRequestUseCase,
            ApproveBookingRequestUseCase approveBookingRequestUseCase,
            RejectBookingRequestUseCase rejectBookingRequestUseCase
    ) {
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.getBookingRequestsUseCase = getBookingRequestsUseCase;
        this.getBookingRequestUseCase = getBookingRequestUseCase;
        this.createBookingRequestUseCase = createBookingRequestUseCase;
        this.approveBookingRequestUseCase = approveBookingRequestUseCase;
        this.rejectBookingRequestUseCase = rejectBookingRequestUseCase;
    }

    @GET
    public List<BookingRequestDTO> getAll(
            @QueryParam("open") Boolean onlyOpen,
            @CookieParam(SESSION_COOKIE_NAME) String sessionId
    ) {
        User currentUser = getCurrentUserUseCase.getCurrentUser(requireSessionId(sessionId));

        if (currentUser.getRole().isAdmin()) {
            if (Boolean.TRUE.equals(onlyOpen)) {
                return getBookingRequestsUseCase.getOpenRequests(currentUser.getId())
                        .stream()
                        .map(BookingRequestApiMapper::toDTO)
                        .toList();
            }
            return getBookingRequestsUseCase.getAllRequests(currentUser.getId())
                    .stream()
                    .map(BookingRequestApiMapper::toDTO)
                    .toList();
        }

        return getBookingRequestsUseCase.getRequestsByUser(currentUser.getId())
                .stream()
                .map(BookingRequestApiMapper::toDTO)
                .toList();
    }

    @GET
    @Path("/{id}")
    public BookingRequestDTO getById(
            @PathParam("id") UUID id,
            @CookieParam(SESSION_COOKIE_NAME) String sessionId
    ) {
        User currentUser = getCurrentUserUseCase.getCurrentUser(requireSessionId(sessionId));
        return BookingRequestApiMapper.toDTO(
                getBookingRequestUseCase.getById(currentUser.getId(), id)
        );
    }

    @POST
    public Response create(
            BookingRequestDTO request,
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

        UUID created = createBookingRequestUseCase.create(
                currentUser.getId(),
                request.hallId(),
                request.title(),
                request.description(),
                request.startDateTime(),
                request.endDateTime()
        );
        return Response.status(Response.Status.CREATED)
                .entity(new EmptyResponseDTO())
                .header("Location", "/booking-requests/" + created)
                .build();
    }

    @POST
    @Path("/{id}/approve")
    public Response approve(
            @PathParam("id") UUID id,
            @CookieParam(SESSION_COOKIE_NAME) String sessionId
    ) {
        User currentUser = getCurrentUserUseCase.getCurrentUser(requireSessionId(sessionId));
        approveBookingRequestUseCase.approve(currentUser.getId(), id);
        return Response.ok(new EmptyResponseDTO()).build();
    }

    @POST
    @Path("/{id}/reject")
    public Response reject(
            @PathParam("id") UUID id,
            RejectionDTO body,
            @CookieParam(SESSION_COOKIE_NAME) String sessionId
    ) {
        User currentUser = getCurrentUserUseCase.getCurrentUser(requireSessionId(sessionId));
        if (body == null) {
            throw new ValidationException("Request body is required");
        }
        String reason = body.reason();
        if (reason == null || reason.isBlank()) {
            throw new ValidationException("reason is required");
        }
        rejectBookingRequestUseCase.reject(currentUser.getId(), id, reason);
        return Response.ok(new EmptyResponseDTO()).build();
    }

    private String requireSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new UnauthorizedException("Missing session cookie");
        }
        return sessionId;
    }
}
