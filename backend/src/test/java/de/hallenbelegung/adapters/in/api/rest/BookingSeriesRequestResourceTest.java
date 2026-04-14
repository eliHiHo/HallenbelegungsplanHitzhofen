package de.hallenbelegung.adapters.in.api.rest;

import de.hallenbelegung.adapters.in.api.dto.BookingSeriesApproveResultDTO;
import de.hallenbelegung.adapters.in.api.dto.BookingSeriesRequestDTO;
import de.hallenbelegung.adapters.in.api.dto.EmptyResponseDTO;
import de.hallenbelegung.adapters.in.api.dto.RejectionDTO;
import de.hallenbelegung.application.domain.exception.UnauthorizedException;
import de.hallenbelegung.application.domain.exception.ValidationException;
import de.hallenbelegung.application.domain.model.BookingRequestStatus;
import de.hallenbelegung.application.domain.model.BookingSeriesRequest;
import de.hallenbelegung.application.domain.model.Hall;
import de.hallenbelegung.application.domain.model.HallType;
import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.in.ApproveBookingSeriesRequestUseCase;
import de.hallenbelegung.application.domain.port.in.CreateBookingSeriesRequestUseCase;
import de.hallenbelegung.application.domain.port.in.GetBookingSeriesRequestUseCase;
import de.hallenbelegung.application.domain.port.in.GetBookingSeriesRequestsUseCase;
import de.hallenbelegung.application.domain.port.in.GetCurrentUserUseCase;
import de.hallenbelegung.application.domain.port.in.RejectBookingSeriesRequestUseCase;
import de.hallenbelegung.application.domain.view.BookingSeriesApproveResult;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingSeriesRequestResourceTest {

    private GetCurrentUserUseCase getCurrentUserUseCase;
    private GetBookingSeriesRequestsUseCase getBookingSeriesRequestsUseCase;
    private GetBookingSeriesRequestUseCase getBookingSeriesRequestUseCase;
    private CreateBookingSeriesRequestUseCase createBookingSeriesRequestUseCase;
    private ApproveBookingSeriesRequestUseCase approveBookingSeriesRequestUseCase;
    private RejectBookingSeriesRequestUseCase rejectBookingSeriesRequestUseCase;

    private BookingSeriesRequestResource resource;

    @BeforeEach
    void setUp() {
        getCurrentUserUseCase = mock(GetCurrentUserUseCase.class);
        getBookingSeriesRequestsUseCase = mock(GetBookingSeriesRequestsUseCase.class);
        getBookingSeriesRequestUseCase = mock(GetBookingSeriesRequestUseCase.class);
        createBookingSeriesRequestUseCase = mock(CreateBookingSeriesRequestUseCase.class);
        approveBookingSeriesRequestUseCase = mock(ApproveBookingSeriesRequestUseCase.class);
        rejectBookingSeriesRequestUseCase = mock(RejectBookingSeriesRequestUseCase.class);

        resource = new BookingSeriesRequestResource(
                getCurrentUserUseCase,
                getBookingSeriesRequestsUseCase,
                getBookingSeriesRequestUseCase,
                createBookingSeriesRequestUseCase,
                approveBookingSeriesRequestUseCase,
                rejectBookingSeriesRequestUseCase
        );
    }

    private User user(Role role) {
        return new User(
                UUID.randomUUID(),
                "Max",
                "Mustermann",
                "max@example.com",
                "hash",
                role,
                true,
                Instant.now(),
                Instant.now()
        );
    }

    private Hall hall() {
        return new Hall(
                UUID.randomUUID(),
                "Halle A",
                "desc",
                true,
                Instant.now(),
                Instant.now(),
                HallType.PART_SMALL
        );
    }

    private BookingSeriesRequest request(UUID id, User requester) {
        return new BookingSeriesRequest(
                id,
                "Serie",
                "desc",
                DayOfWeek.MONDAY,
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
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
    void getAll_for_admin_uses_open_or_all_flag() {
        User admin = user(Role.ADMIN);
        BookingSeriesRequest openRequest = request(UUID.randomUUID(), admin);

        when(getCurrentUserUseCase.getCurrentUser("sess")).thenReturn(admin);
        when(getBookingSeriesRequestsUseCase.getOpenRequests(admin.getId())).thenReturn(List.of(openRequest));
        when(getBookingSeriesRequestsUseCase.getAllRequests(admin.getId())).thenReturn(List.of(openRequest));

        List<BookingSeriesRequestDTO> open = resource.getAll(true, "sess");
        List<BookingSeriesRequestDTO> all = resource.getAll(false, "sess");

        assertEquals(1, open.size());
        assertEquals(openRequest.getId(), open.get(0).id());
        assertEquals(1, all.size());
        assertEquals(openRequest.getId(), all.get(0).id());

        verify(getBookingSeriesRequestsUseCase).getOpenRequests(admin.getId());
        verify(getBookingSeriesRequestsUseCase).getAllRequests(admin.getId());
    }

    @Test
    void getAll_for_representative_uses_own_requests() {
        User representative = user(Role.CLUB_REPRESENTATIVE);
        BookingSeriesRequest own = request(UUID.randomUUID(), representative);

        when(getCurrentUserUseCase.getCurrentUser("sess")).thenReturn(representative);
        when(getBookingSeriesRequestsUseCase.getRequestsByUser(representative.getId())).thenReturn(List.of(own));

        List<BookingSeriesRequestDTO> result = resource.getAll(null, "sess");

        assertEquals(1, result.size());
        assertEquals(own.getId(), result.get(0).id());
        verify(getBookingSeriesRequestsUseCase).getRequestsByUser(representative.getId());
    }

    @Test
    void getAll_rejects_missing_cookie() {
        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> resource.getAll(false, " "));
        assertEquals("Missing session cookie", ex.getMessage());
    }

    @Test
    void getById_maps_result() {
        User representative = user(Role.CLUB_REPRESENTATIVE);
        UUID requestId = UUID.randomUUID();
        BookingSeriesRequest found = request(requestId, representative);

        when(getCurrentUserUseCase.getCurrentUser("sess")).thenReturn(representative);
        when(getBookingSeriesRequestUseCase.getById(representative.getId(), requestId)).thenReturn(found);

        BookingSeriesRequestDTO dto = resource.getById(requestId, "sess");

        assertEquals(requestId, dto.id());
        assertEquals("Serie", dto.title());
    }

    @Test
    void create_validates_input_and_returns_created_with_location() {
        User representative = user(Role.CLUB_REPRESENTATIVE);
        UUID createdId = UUID.randomUUID();

        BookingSeriesRequestDTO body = new BookingSeriesRequestDTO(
                null,
                "Serie",
                "desc",
                DayOfWeek.MONDAY,
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 1),
                UUID.randomUUID(),
                null,
                null,
                null,
                null
        );

        when(getCurrentUserUseCase.getCurrentUser("sess")).thenReturn(representative);
        when(createBookingSeriesRequestUseCase.create(
                representative.getId(),
                body.hallId(),
                body.title(),
                body.description(),
                body.weekday(),
                body.startTime(),
                body.endTime(),
                body.startDate(),
                body.endDate()
        )).thenReturn(createdId);

        Response response = resource.create(body, "sess");

        assertEquals(201, response.getStatus());
        assertInstanceOf(EmptyResponseDTO.class, response.getEntity());
        assertEquals("/booking-series-requests/" + createdId, response.getHeaderString("Location"));
    }

    @Test
    void create_rejects_missing_required_fields() {
        User representative = user(Role.CLUB_REPRESENTATIVE);
        when(getCurrentUserUseCase.getCurrentUser("sess")).thenReturn(representative);

        ValidationException ex1 = assertThrows(ValidationException.class, () -> resource.create(null, "sess"));
        assertEquals("Request body is required", ex1.getMessage());

        BookingSeriesRequestDTO missingHall = new BookingSeriesRequestDTO(
                null, "Serie", null, DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalTime.of(19, 0),
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1), null, null, null, null, null
        );
        ValidationException ex2 = assertThrows(ValidationException.class, () -> resource.create(missingHall, "sess"));
        assertEquals("hallId is required", ex2.getMessage());
    }

    @Test
    void approve_maps_result_dto() {
        User admin = user(Role.ADMIN);
        UUID id = UUID.randomUUID();
        BookingSeriesApproveResult result = new BookingSeriesApproveResult(
                List.of(UUID.randomUUID()),
                List.of(LocalDate.of(2026, 5, 5))
        );

        when(getCurrentUserUseCase.getCurrentUser("sess")).thenReturn(admin);
        when(approveBookingSeriesRequestUseCase.approve(admin.getId(), id)).thenReturn(result);

        Response response = resource.approve(id, "sess");

        assertEquals(200, response.getStatus());
        assertInstanceOf(BookingSeriesApproveResultDTO.class, response.getEntity());
        BookingSeriesApproveResultDTO dto = (BookingSeriesApproveResultDTO) response.getEntity();
        assertEquals(1, dto.createdBookingIds().size());
        assertEquals(1, dto.skippedOccurrences().size());
    }

    @Test
    void reject_validates_reason_and_calls_use_case() {
        User admin = user(Role.ADMIN);
        UUID id = UUID.randomUUID();

        when(getCurrentUserUseCase.getCurrentUser("sess")).thenReturn(admin);

        ValidationException ex1 = assertThrows(ValidationException.class, () -> resource.reject(id, null, "sess"));
        assertEquals("Request body is required", ex1.getMessage());

        ValidationException ex2 = assertThrows(
                ValidationException.class,
                () -> resource.reject(id, new RejectionDTO("   "), "sess")
        );
        assertEquals("reason is required", ex2.getMessage());

        Response response = resource.reject(id, new RejectionDTO("Konflikt"), "sess");
        assertEquals(200, response.getStatus());
        assertInstanceOf(EmptyResponseDTO.class, response.getEntity());
        verify(rejectBookingSeriesRequestUseCase).reject(admin.getId(), id, "Konflikt");
    }
}

