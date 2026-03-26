package de.hallenbelegung.adapters.in.api.rest;

import de.hallenbelegung.adapters.in.api.dto.HallStatisticsDTO;
import de.hallenbelegung.adapters.in.api.dto.SeriesStatisticsDTO;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.in.GetCurrentUserUseCase;
import de.hallenbelegung.application.domain.port.in.GetHallStatisticsUseCase;
import de.hallenbelegung.application.domain.port.in.GetSeriesStatisticsOverviewUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
@Path("/statistics")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class StatisticsResource {

    private static final String SESSION_COOKIE_NAME = "HB_SESSION";

    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final GetHallStatisticsUseCase getHallStatisticsUseCase;
    private final GetSeriesStatisticsOverviewUseCase getSeriesStatisticsOverviewUseCase;

    // NOTE: GetSeriesStatisticsDetailUseCase is NOT injected here because no suitable
    // response DTO exists for SeriesStatisticsDetailView. The endpoint
    // GET /statistics/series/{id} is intentionally not implemented yet.
    // See "Offene Lücken" in the implementation documentation.

    public StatisticsResource(
            GetCurrentUserUseCase getCurrentUserUseCase,
            GetHallStatisticsUseCase getHallStatisticsUseCase,
            GetSeriesStatisticsOverviewUseCase getSeriesStatisticsOverviewUseCase
    ) {
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.getHallStatisticsUseCase = getHallStatisticsUseCase;
        this.getSeriesStatisticsOverviewUseCase = getSeriesStatisticsOverviewUseCase;
    }

    /**
     * Returns hall utilisation statistics for the given time range.
     *
     * WARNING – DTO gap: HallStatisticsDTO currently only carries hallName and bookingCount.
     * The underlying HallStatisticsView also contains cancelledBookings, totalParticipants,
     * utilizationPercent and topSeries, which are silently dropped here.
     * HallStatisticsDTO must be enriched before this endpoint can return complete data.
     *
     * @param from  start of the period (inclusive), defaults to first day of current year
     * @param to    end of the period (inclusive), defaults to today
     */
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
                .map(v -> new HallStatisticsDTO(v.getHallName(), v.getTotalBookings()))
                .toList();
    }

    /**
     * Returns a summary overview of all booking series with their usage statistics.
     *
     * WARNING – DTO gap: SeriesStatisticsDTO currently only carries title and bookingCount.
     * The underlying SeriesStatisticsOverviewView also contains hallName, conductedAppointments,
     * cancelledAppointments, totalParticipants and averageParticipants, which are dropped here.
     * SeriesStatisticsDTO must be enriched before this endpoint can return complete data.
     *
     * @param from  start of the period (inclusive), defaults to first day of current year
     * @param to    end of the period (inclusive), defaults to today
     */
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
                .map(v -> new SeriesStatisticsDTO(v.getTitle(), v.getTotalAppointments()))
                .toList();
    }

    // GET /statistics/series/{id} is NOT implemented here.
    // Reason: GetSeriesStatisticsDetailUseCase returns SeriesStatisticsDetailView,
    // which contains a rich structure (occurrences list, averageParticipants, etc.)
    // for which no matching response DTO exists in the project.
    // A dedicated SeriesStatisticsDetailDTO (mirroring SeriesStatisticsDetailView)
    // must be added before this endpoint can be implemented.

    private String requireSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Missing session cookie");
        }
        return sessionId;
    }
}