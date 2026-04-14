package de.hallenbelegung.adapters.in.api.rest;

import de.hallenbelegung.adapters.in.api.dto.HallStatisticsDTO;
import de.hallenbelegung.adapters.in.api.dto.SeriesOccurrenceDTO;
import de.hallenbelegung.adapters.in.api.dto.SeriesStatisticsDTO;
import de.hallenbelegung.adapters.in.api.dto.SeriesStatisticsDetailDTO;
import de.hallenbelegung.adapters.in.api.dto.SeriesUsageDTO;
import de.hallenbelegung.application.domain.exception.UnauthorizedException;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.in.GetCurrentUserUseCase;
import de.hallenbelegung.application.domain.port.in.GetHallStatisticsUseCase;
import de.hallenbelegung.application.domain.port.in.GetSeriesStatisticsDetailUseCase;
import de.hallenbelegung.application.domain.port.in.GetSeriesStatisticsOverviewUseCase;
import de.hallenbelegung.application.domain.view.HallStatisticsView;
import de.hallenbelegung.application.domain.view.SeriesOccurrenceStatisticsView;
import de.hallenbelegung.application.domain.view.SeriesStatisticsDetailView;
import de.hallenbelegung.application.domain.view.SeriesStatisticsOverviewView;
import de.hallenbelegung.application.domain.view.SeriesUsageView;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
@Path("/statistics")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class StatisticsResource {

    private static final String SESSION_COOKIE_NAME = "HB_SESSION";

    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final GetHallStatisticsUseCase getHallStatisticsUseCase;
    private final GetSeriesStatisticsOverviewUseCase getSeriesStatisticsOverviewUseCase;
    private final GetSeriesStatisticsDetailUseCase getSeriesStatisticsDetailUseCase;

    public StatisticsResource(
            GetCurrentUserUseCase getCurrentUserUseCase,
            GetHallStatisticsUseCase getHallStatisticsUseCase,
            GetSeriesStatisticsOverviewUseCase getSeriesStatisticsOverviewUseCase,
            GetSeriesStatisticsDetailUseCase getSeriesStatisticsDetailUseCase
    ) {
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.getHallStatisticsUseCase = getHallStatisticsUseCase;
        this.getSeriesStatisticsOverviewUseCase = getSeriesStatisticsOverviewUseCase;
        this.getSeriesStatisticsDetailUseCase = getSeriesStatisticsDetailUseCase;
    }

    @GET
    @Path("/halls")
    public List<HallStatisticsDTO> getHallStatistics(
            @QueryParam("from") LocalDate from,
            @QueryParam("to") LocalDate to,
            @CookieParam(SESSION_COOKIE_NAME) String sessionId
    ) {
        User currentUser = getCurrentUserUseCase.getCurrentUser(requireSessionId(sessionId));

        LocalDate effectiveFrom = from != null ? from : LocalDate.now().withDayOfYear(1);
        LocalDate effectiveTo = to != null ? to : LocalDate.now();

        return getHallStatisticsUseCase
                .getHallStatistics(currentUser.getId(), effectiveFrom, effectiveTo)
                .stream()
                .map(this::toHallStatisticsDTO)
                .toList();
    }

    @GET
    @Path("/series")
    public List<SeriesStatisticsDTO> getSeriesStatisticsOverview(
            @QueryParam("from") LocalDate from,
            @QueryParam("to") LocalDate to,
            @CookieParam(SESSION_COOKIE_NAME) String sessionId
    ) {
        User currentUser = getCurrentUserUseCase.getCurrentUser(requireSessionId(sessionId));

        LocalDate effectiveFrom = from != null ? from : LocalDate.now().withDayOfYear(1);
        LocalDate effectiveTo = to != null ? to : LocalDate.now();

        return getSeriesStatisticsOverviewUseCase
                .getSeriesStatisticsOverview(currentUser.getId(), effectiveFrom, effectiveTo)
                .stream()
                .map(this::toSeriesStatisticsDTO)
                .toList();
    }

    @GET
    @Path("/series/{id}")
    public SeriesStatisticsDetailDTO getSeriesStatisticsDetail(
            @PathParam("id") UUID id,
            @QueryParam("from") LocalDate from,
            @QueryParam("to") LocalDate to,
            @CookieParam(SESSION_COOKIE_NAME) String sessionId
    ) {
        User currentUser = getCurrentUserUseCase.getCurrentUser(requireSessionId(sessionId));

        LocalDate effectiveFrom = from != null ? from : LocalDate.now().withDayOfYear(1);
        LocalDate effectiveTo = to != null ? to : LocalDate.now();

        SeriesStatisticsDetailView view = getSeriesStatisticsDetailUseCase.getSeriesStatisticsDetail(
                currentUser.getId(), id, effectiveFrom, effectiveTo
        );

        return toSeriesStatisticsDetailDTO(view);
    }

    private HallStatisticsDTO toHallStatisticsDTO(HallStatisticsView v) {
        List<SeriesUsageDTO> top = v.getTopSeries().stream()
                .map(this::toSeriesUsageDTO)
                .toList();

        return new HallStatisticsDTO(
                v.getHallId(),
                v.getHallName(),
                v.getTotalBookings(),
                v.getCancelledBookings(),
                v.getTotalParticipants(),
                v.getUtilizationPercent(),
                top
        );
    }

    private SeriesUsageDTO toSeriesUsageDTO(SeriesUsageView v) {
        return new SeriesUsageDTO(
                v.getBookingSeriesId(),
                v.getTitle(),
                v.getBookingCount()
        );
    }

    private SeriesStatisticsDTO toSeriesStatisticsDTO(SeriesStatisticsOverviewView v) {
        return new SeriesStatisticsDTO(
                v.getBookingSeriesId(),
                v.getTitle(),
                v.getHallName(),
                v.getTotalAppointments(),
                v.getConductedAppointments(),
                v.getCancelledAppointments(),
                v.getTotalParticipants(),
                v.getAverageParticipants()
        );
    }

    private SeriesOccurrenceDTO toSeriesOccurrenceDTO(SeriesOccurrenceStatisticsView v) {
        return new SeriesOccurrenceDTO(
                v.getBookingId(),
                v.getStartDateTime(),
                v.getEndDateTime(),
                v.isCancelled(),
                v.isConducted(),
                v.getParticipantCount(),
                v.getFeedbackComment()
        );
    }

    private SeriesStatisticsDetailDTO toSeriesStatisticsDetailDTO(SeriesStatisticsDetailView v) {
        List<SeriesOccurrenceDTO> occ = v.getOccurrences().stream()
                .map(this::toSeriesOccurrenceDTO)
                .toList();

        return new SeriesStatisticsDetailDTO(
                v.getBookingSeriesId(),
                v.getTitle(),
                v.getHallName(),
                v.getResponsibleUserName(),
                v.getTotalAppointments(),
                v.getConductedAppointments(),
                v.getCancelledAppointments(),
                v.getTotalParticipants(),
                v.getAverageParticipants(),
                occ
        );
    }

    private String requireSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new UnauthorizedException("Missing session cookie");
        }
        return sessionId;
    }
}

