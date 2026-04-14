package de.hallenbelegung.adapters.in.api.rest;

import de.hallenbelegung.adapters.in.api.dto.BookingRequestDTO;
import de.hallenbelegung.adapters.in.api.dto.EmptyResponseDTO;
import de.hallenbelegung.adapters.in.api.dto.RejectionDTO;
import de.hallenbelegung.application.domain.exception.UnauthorizedException;
import de.hallenbelegung.application.domain.exception.ValidationException;
import de.hallenbelegung.application.domain.model.BookingRequest;
import de.hallenbelegung.application.domain.model.BookingRequestStatus;
import de.hallenbelegung.application.domain.model.Hall;
import de.hallenbelegung.application.domain.model.HallType;
import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.in.ApproveBookingRequestUseCase;
import de.hallenbelegung.application.domain.port.in.CreateBookingRequestUseCase;
import de.hallenbelegung.application.domain.port.in.GetBookingRequestUseCase;
import de.hallenbelegung.application.domain.port.in.GetBookingRequestsUseCase;
import de.hallenbelegung.application.domain.port.in.GetCurrentUserUseCase;
import de.hallenbelegung.application.domain.port.in.RejectBookingRequestUseCase;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingRequestResourceTest {

    private GetCurrentUserUseCase getCurrentUserUseCase;
    private GetBookingRequestsUseCase getBookingRequestsUseCase;
    private GetBookingRequestUseCase getBookingRequestUseCase;
    private CreateBookingRequestUseCase createBookingRequestUseCase;
    private ApproveBookingRequestUseCase approveBookingRequestUseCase;
    private RejectBookingRequestUseCase rejectBookingRequestUseCase;

    private BookingRequestResource resource;

    @BeforeEach
    void setUp() {
        getCurrentUserUseCase = mock(GetCurrentUserUseCase.class);
        getBookingRequestsUseCase = mock(GetBookingRequestsUseCase.class);
        getBookingRequestUseCase = mock(GetBookingRequestUseCase.class);
        createBookingRequestUseCase = mock(CreateBookingRequestUseCase.class);
        approveBookingRequestUseCase = mock(ApproveBookingRequestUseCase.class);
        rejectBookingRequestUseCase = mock(RejectBookingRequestUseCase.class);

        resource = new BookingRequestResource(
                getCurrentUserUseCase,
                getBookingRequestsUseCase,
                getBookingRequestUseCase,
                createBookingRequestUseCase,
                approveBookingRequestUseCase,
                rejectBookingRequestUseCase
        );
    }

    private User user(Role role) {
        return new User(UUID.randomUUID(), "Max", "Mustermann", "max@example.com", "hash", role, true, Instant.now(), Instant.now());
    }

    private Hall hall() {
        return new Hall(UUID.randomUUID(), "Halle A", "desc", true, Instant.now(), Instant.now(), HallType.PART_SMALL);
    }

    private BookingRequest request(UUID id, User requester) {
        return new BookingRequest(
                id,
                "Anfrage",
                "desc",
                LocalDateTime.of(2026, 5, 4, 10, 0),
                LocalDateTime.of(2026, 5, 4, 11, 0),
                BookingRequestStatus.PENDING,
                null,
                hall(),
                requester,
                null,
                Instant.now(),
                Instant.now(),
                null
        );
    }

    @Test
    void getAll_admin_uses_open_or_all_branch() {
        User admin = user(Role.ADMIN);
        BookingRequest req = request(UUID.randomUUID(), admin);

        when(getCurrentUserUseCase.getCurrentUser("sess")).thenReturn(admin);
        when(getBookingRequestsUseCase.getOpenRequests(admin.getId())).thenReturn(List.of(req));
        when(getBookingRequestsUseCase.getAllRequests(admin.getId())).thenReturn(List.of(req));

        List<BookingRequestDTO> open = resource.getAll(true, "sess");
        List<BookingRequestDTO> all = resource.getAll(false, "sess");

        assertEquals(1, open.size());
        assertEquals(1, all.size());
        verify(getBookingRequestsUseCase).getOpenRequests(admin.getId());
        verify(getBookingRequestsUseCase).getAllRequests(admin.getId());
    }

    @Test
    void getAll_representative_sees_only_own() {
        User rep = user(Role.CLUB_REPRESENTATIVE);
        BookingRequest req = request(UUID.randomUUID(), rep);

        when(getCurrentUserUseCase.getCurrentUser("sess")).thenReturn(rep);
        when(getBookingRequestsUseCase.getRequestsByUser(rep.getId())).thenReturn(List.of(req));

        List<BookingRequestDTO> result = resource.getAll(null, "sess");

        assertEquals(1, result.size());
        verify(getBookingRequestsUseCase).getRequestsByUser(rep.getId());
    }

    @Test
    void getById_requires_cookie() {
        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> resource.getById(UUID.randomUUID(), " ")
        );
        assertEquals("Missing session cookie", ex.getMessage());
    }

    @Test
    void create_validates_and_returns_created_location() {
        User rep = user(Role.CLUB_REPRESENTATIVE);
        UUID hallId = UUID.randomUUID();
        UUID createdId = UUID.randomUUID();

        BookingRequestDTO body = new BookingRequestDTO(
                null,
                "Anfrage",
                "desc",
                LocalDateTime.of(2026, 5, 4, 10, 0),
                LocalDateTime.of(2026, 5, 4, 11, 0),
                hallId,
                null,
                null,
                null,
                null
        );

        when(getCurrentUserUseCase.getCurrentUser("sess")).thenReturn(rep);
        when(createBookingRequestUseCase.create(rep.getId(), hallId, "Anfrage", "desc", body.startDateTime(), body.endDateTime()))
                .thenReturn(createdId);

        Response response = resource.create(body, "sess");

        assertEquals(201, response.getStatus());
        assertInstanceOf(EmptyResponseDTO.class, response.getEntity());
        assertEquals("/booking-requests/" + createdId, response.getHeaderString("Location"));
    }

    @Test
    void create_rejects_missing_fields() {
        User rep = user(Role.CLUB_REPRESENTATIVE);
        when(getCurrentUserUseCase.getCurrentUser("sess")).thenReturn(rep);

        ValidationException ex1 = assertThrows(ValidationException.class, () -> resource.create(null, "sess"));
        assertEquals("Request body is required", ex1.getMessage());

        BookingRequestDTO missingHall = new BookingRequestDTO(
                null,
                "Anfrage",
                null,
                LocalDateTime.of(2026, 5, 4, 10, 0),
                LocalDateTime.of(2026, 5, 4, 11, 0),
                null,
                null,
                null,
                null,
                null
        );

        ValidationException ex2 = assertThrows(ValidationException.class, () -> resource.create(missingHall, "sess"));
        assertEquals("hallId is required", ex2.getMessage());
    }

    @Test
    void approve_requires_cookie_and_calls_use_case() {
        UUID requestId = UUID.randomUUID();
        User admin = user(Role.ADMIN);

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> resource.approve(requestId, null)
        );
        assertEquals("Missing session cookie", ex.getMessage());

        when(getCurrentUserUseCase.getCurrentUser("sess")).thenReturn(admin);
        Response response = resource.approve(requestId, "sess");

        assertEquals(200, response.getStatus());
        assertInstanceOf(EmptyResponseDTO.class, response.getEntity());
        verify(approveBookingRequestUseCase).approve(admin.getId(), requestId);
    }

    @Test
    void reject_requires_non_blank_reason() {
        UUID requestId = UUID.randomUUID();
        User admin = user(Role.ADMIN);
        when(getCurrentUserUseCase.getCurrentUser("sess")).thenReturn(admin);

        ValidationException ex1 = assertThrows(ValidationException.class, () -> resource.reject(requestId, null, "sess"));
        assertEquals("Request body is required", ex1.getMessage());

        ValidationException ex2 = assertThrows(
                ValidationException.class,
                () -> resource.reject(requestId, new RejectionDTO("  "), "sess")
        );
        assertEquals("reason is required", ex2.getMessage());

        Response response = resource.reject(requestId, new RejectionDTO("Konflikt"), "sess");
        assertEquals(200, response.getStatus());
        assertInstanceOf(EmptyResponseDTO.class, response.getEntity());
        verify(rejectBookingRequestUseCase).reject(admin.getId(), requestId, "Konflikt");
    }
}

