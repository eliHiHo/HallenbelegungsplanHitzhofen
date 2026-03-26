package de.hallenbelegung.adapters.in.api.rest;

import de.hallenbelegung.adapters.in.api.dto.CalendarDayDTO;
import de.hallenbelegung.adapters.in.api.dto.CalendarWeekDTO;
import de.hallenbelegung.adapters.in.api.mapper.CalendarApiMapper;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.in.GetCalendarDayUseCase;
import de.hallenbelegung.application.domain.port.in.GetCalendarWeekUseCase;
import de.hallenbelegung.application.domain.port.in.GetCurrentUserUseCase;
import de.hallenbelegung.application.domain.view.CalendarDayView;
import de.hallenbelegung.application.domain.view.CalendarWeekView;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.time.LocalDate;
import java.util.UUID;

@ApplicationScoped
@Path("/calendar")
@Produces(MediaType.APPLICATION_JSON)
public class CalendarResource {

    private static final String SESSION_COOKIE_NAME = "HB_SESSION";

    private final GetCalendarWeekUseCase getCalendarWeekUseCase;
    private final GetCalendarDayUseCase getCalendarDayUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;

    public CalendarResource(
            GetCalendarWeekUseCase getCalendarWeekUseCase,
            GetCalendarDayUseCase getCalendarDayUseCase,
            GetCurrentUserUseCase getCurrentUserUseCase
    ) {
        this.getCalendarWeekUseCase = getCalendarWeekUseCase;
        this.getCalendarDayUseCase = getCalendarDayUseCase;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
    }

    @GET
    @Path("/week")
    public CalendarWeekDTO getWeek(
            @QueryParam("weekStart") LocalDate weekStart,
            @CookieParam(SESSION_COOKIE_NAME) String sessionId
    ) {
        LocalDate effectiveWeekStart = weekStart != null ? weekStart : LocalDate.now();
        UUID currentUserId = resolveCurrentUserId(sessionId);

        CalendarWeekView view = getCalendarWeekUseCase.getWeek(effectiveWeekStart, currentUserId);
        return CalendarApiMapper.toDTO(view);
    }

    @GET
    @Path("/day")
    public CalendarDayDTO getDay(
            @QueryParam("day") LocalDate day,
            @CookieParam(SESSION_COOKIE_NAME) String sessionId
    ) {
        LocalDate effectiveDay = day != null ? day : LocalDate.now();
        UUID currentUserId = resolveCurrentUserId(sessionId);

        CalendarDayView view = getCalendarDayUseCase.getDay(effectiveDay, currentUserId);
        return CalendarApiMapper.toDTO(view);
    }

    private UUID resolveCurrentUserId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }

        User currentUser = getCurrentUserUseCase.getCurrentUser(sessionId);
        return currentUser.getId();
    }
}