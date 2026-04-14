package de.hallenbelegung.adapters.in.api.rest;

import de.hallenbelegung.adapters.in.api.dto.HallStatisticsDTO;
import de.hallenbelegung.adapters.in.api.dto.SeriesStatisticsDTO;
import de.hallenbelegung.adapters.in.api.dto.SeriesStatisticsDetailDTO;
import de.hallenbelegung.application.domain.exception.UnauthorizedException;
import de.hallenbelegung.application.domain.model.Role;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StatisticsResourceTest {

    private GetCurrentUserUseCase getCurrentUserUseCase;
    private GetHallStatisticsUseCase getHallStatisticsUseCase;
    private GetSeriesStatisticsOverviewUseCase getSeriesStatisticsOverviewUseCase;
    private GetSeriesStatisticsDetailUseCase getSeriesStatisticsDetailUseCase;

    private StatisticsResource resource;

    @BeforeEach
    void setUp() {
        getCurrentUserUseCase = mock(GetCurrentUserUseCase.class);
        getHallStatisticsUseCase = mock(GetHallStatisticsUseCase.class);
        getSeriesStatisticsOverviewUseCase = mock(GetSeriesStatisticsOverviewUseCase.class);
        getSeriesStatisticsDetailUseCase = mock(GetSeriesStatisticsDetailUseCase.class);

        resource = new StatisticsResource(
                getCurrentUserUseCase,
                getHallStatisticsUseCase,
                getSeriesStatisticsOverviewUseCase,
                getSeriesStatisticsDetailUseCase
        );
    }

    private User admin(UUID id) {
        return new User(id, "Admin", "User", "admin@example.com", "hash", Role.ADMIN, true, Instant.now(), Instant.now());
    }

    @Test
    void getHallStatistics_requires_cookie_and_maps_nested_top_series() {
        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> resource.getHallStatistics(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), " ")
        );
        assertEquals("Missing session cookie", ex.getMessage());

        UUID userId = UUID.randomUUID();
        UUID hallId = UUID.randomUUID();
        UUID seriesId = UUID.randomUUID();
        when(getCurrentUserUseCase.getCurrentUser("sess")).thenReturn(admin(userId));
        when(getHallStatisticsUseCase.getHallStatistics(
                userId,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31)
        )).thenReturn(List.of(new HallStatisticsView(
                hallId,
                "Halle A",
                10,
                2,
                120,
                75.0,
                List.of(new SeriesUsageView(seriesId, "Jugend", 6))
        )));

        List<HallStatisticsDTO> result = resource.getHallStatistics(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                "sess"
        );

        assertEquals(1, result.size());
        assertEquals(hallId, result.get(0).hallId());
        assertEquals(1, result.get(0).topSeries().size());
        assertEquals(seriesId, result.get(0).topSeries().get(0).bookingSeriesId());
    }

    @Test
    void getSeriesStatisticsOverview_uses_given_range_and_maps() {
        UUID userId = UUID.randomUUID();
        UUID seriesId = UUID.randomUUID();
        when(getCurrentUserUseCase.getCurrentUser("sess")).thenReturn(admin(userId));
        when(getSeriesStatisticsOverviewUseCase.getSeriesStatisticsOverview(
                userId,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31)
        )).thenReturn(List.of(new SeriesStatisticsOverviewView(
                seriesId,
                "Jugend",
                "Halle A",
                20,
                18,
                2,
                250,
                13.8
        )));

        List<SeriesStatisticsDTO> result = resource.getSeriesStatisticsOverview(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                "sess"
        );

        assertEquals(1, result.size());
        assertEquals(seriesId, result.get(0).bookingSeriesId());
        assertEquals(20, result.get(0).totalAppointments());
    }

    @Test
    void getSeriesStatisticsDetail_maps_occurrences_and_defaults_date_range_when_null() {
        UUID userId = UUID.randomUUID();
        UUID seriesId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        when(getCurrentUserUseCase.getCurrentUser("sess")).thenReturn(admin(userId));

        when(getSeriesStatisticsDetailUseCase.getSeriesStatisticsDetail(eq(userId), eq(seriesId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new SeriesStatisticsDetailView(
                        seriesId,
                        "Jugend",
                        "Halle A",
                        "Max Mustermann",
                        20,
                        18,
                        2,
                        250,
                        13.8,
                        List.of(new SeriesOccurrenceStatisticsView(
                                bookingId,
                                LocalDateTime.of(2026, 5, 4, 10, 0),
                                LocalDateTime.of(2026, 5, 4, 11, 0),
                                false,
                                true,
                                14,
                                "ok"
                        ))
                ));

        SeriesStatisticsDetailDTO dto = resource.getSeriesStatisticsDetail(seriesId, null, null, "sess");

        assertEquals(seriesId, dto.bookingSeriesId());
        assertEquals(1, dto.occurrences().size());
        assertEquals(bookingId, dto.occurrences().get(0).bookingId());
        verify(getSeriesStatisticsDetailUseCase).getSeriesStatisticsDetail(eq(userId), eq(seriesId), any(LocalDate.class), any(LocalDate.class));
    }
}

