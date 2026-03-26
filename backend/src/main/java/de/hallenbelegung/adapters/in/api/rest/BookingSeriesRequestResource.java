package de.hallenbelegung.adapters.in.api.rest;

import de.hallenbelegung.adapters.in.api.dto.BookingSeriesApproveResultDTO;
import de.hallenbelegung.adapters.in.api.dto.BookingSeriesRequestDTO;
import de.hallenbelegung.adapters.in.api.dto.EmptyResponseDTO;
import de.hallenbelegung.adapters.in.api.dto.RejectionDTO;
import de.hallenbelegung.adapters.in.api.mapper.BookingSeriesRequestApiMapper;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.in.ApproveBookingSeriesRequestUseCase;
import de.hallenbelegung.application.domain.port.in.CreateBookingSeriesRequestUseCase;
import de.hallenbelegung.application.domain.port.in.GetBookingSeriesRequestUseCase;
import de.hallenbelegung.application.domain.port.in.GetBookingSeriesRequestsUseCase;
import de.hallenbelegung.application.domain.port.in.GetCurrentUserUseCase;
import de.hallenbelegung.application.domain.port.in.RejectBookingSeriesRequestUseCase;
import de.hallenbelegung.application.domain.view.BookingSeriesApproveResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
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
@Path("/booking-series-requests")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class BookingSeriesRequestResource {

    private static final String SESSION_COOKIE_NAME = "HB_SESSION";

    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final GetBookingSeriesRequestsUseCase getBookingSeriesRequestsUseCase;
    private final GetBookingSeriesRequestUseCase getBookingSeriesRequestUseCase;
    private final CreateBookingSeriesRequestUseCase createBookingSeriesRequestUseCase;
    private final ApproveBookingSeriesRequestUseCase approveBookingSeriesRequestUseCase;
    private final RejectBookingSeriesRequestUseCase rejectBookingSeriesRequestUseCase;

    public BookingSeriesRequestResource(
            GetCurrentUserUseCase getCurrentUserUseCase,
            GetBookingSeriesRequestsUseCase getBookingSeriesRequestsUseCase,
            GetBookingSeriesRequestUseCase getBookingSeriesRequestUseCase,
            CreateBookingSeriesRequestUseCase createBookingSeriesRequestUseCase,
            ApproveBookingSeriesRequestUseCase approveBookingSeriesRequestUseCase,
            RejectBookingSeriesRequestUseCase rejectBookingSeriesRequestUseCase
    ) {
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.getBookingSeriesRequestsUseCase = getBookingSeriesRequestsUseCase;
        this.getBookingSeriesRequestUseCase = getBookingSeriesRequestUseCase;
        this.createBookingSeriesRequestUseCase = createBookingSeriesRequestUseCase;
        this.approveBookingSeriesRequestUseCase = approveBookingSeriesRequestUseCase;
        this.rejectBookingSeriesRequestUseCase = rejectBookingSeriesRequestUseCase;
    }

    @GET
    public List<BookingSeriesRequestDTO> getAll(
            @QueryParam("open") Boolean onlyOpen,
            @CookieParam(SESSION_COOKIE_NAME) String sessionId
    ) {
        User currentUser = getCurrentUserUseCase.getCurrentUser(requireSessionId(sessionId));

        if (currentUser.getRole().isAdmin()) {
            if (Boolean.TRUE.equals(onlyOpen)) {
                return getBookingSeriesRequestsUseCase.getOpenRequests(currentUser.getId())
                        .stream()
                        .map(BookingSeriesRequestApiMapper::toDTO)
                        .toList();
            }
            return getBookingSeriesRequestsUseCase.getAllRequests(currentUser.getId())
                    .stream()
                    .map(BookingSeriesRequestApiMapper::toDTO)
                    .toList();
        }

        return getBookingSeriesRequestsUseCase.getRequestsByUser(currentUser.getId())
                .stream()
                .map(BookingSeriesRequestApiMapper::toDTO)
                .toList();
    }

    @GET
    @Path("/{id}")
    public BookingSeriesRequestDTO getById(
            @PathParam("id") UUID id,
            @CookieParam(SESSION_COOKIE_NAME) String sessionId
    ) {
        User currentUser = getCurrentUserUseCase.getCurrentUser(requireSessionId(sessionId));
        return BookingSeriesRequestApiMapper.toDTO(
                getBookingSeriesRequestUseCase.getById(currentUser.getId(), id)
        );
    }

    @POST
    public Response create(
            BookingSeriesRequestDTO request,
            @CookieParam(SESSION_COOKIE_NAME) String sessionId
    ) {
        User currentUser = getCurrentUserUseCase.getCurrentUser(requireSessionId(sessionId));
        UUID created = createBookingSeriesRequestUseCase.create(
                currentUser.getId(),
                request.hallId(),
                request.title(),
                request.description(),
                request.weekday(),
                request.startTime(),
                request.endTime(),
                request.startDate(),
                request.endDate()
        );
        return Response.status(Response.Status.CREATED)
                .entity(new EmptyResponseDTO())
                .header("Location", "/booking-series-requests/" + created)
                .build();
    }

    @POST
    @Path("/{id}/approve")
    public Response approve(
            @PathParam("id") UUID id,
            @CookieParam(SESSION_COOKIE_NAME) String sessionId
    ) {
        User currentUser = getCurrentUserUseCase.getCurrentUser(requireSessionId(sessionId));
        BookingSeriesApproveResult result = approveBookingSeriesRequestUseCase.approve(currentUser.getId(), id);
        BookingSeriesApproveResultDTO dto = BookingSeriesRequestApiMapper.toDTO(result);
        return Response.ok(dto).build();
    }

    @POST
    @Path("/{id}/reject")
    public Response reject(
            @PathParam("id") UUID id,
            RejectionDTO body,
            @CookieParam(SESSION_COOKIE_NAME) String sessionId
    ) {
        User currentUser = getCurrentUserUseCase.getCurrentUser(requireSessionId(sessionId));
        String reason = body != null ? body.reason() : null;
        rejectBookingSeriesRequestUseCase.reject(currentUser.getId(), id, reason);
        return Response.ok(new EmptyResponseDTO()).build();
    }

    private String requireSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Missing session cookie");
        }
        return sessionId;
    }
}