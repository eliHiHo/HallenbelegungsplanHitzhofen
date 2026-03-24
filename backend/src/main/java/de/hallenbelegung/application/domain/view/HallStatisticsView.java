package de.hallenbelegung.application.domain.view;

import java.util.List;

public class HallStatisticsView {

    private final Long hallId;
    private final String hallName;
    private final long totalBookings;
    private final long cancelledBookings;
    private final long totalParticipants;
    private final double utilizationPercent;
    private final List<SeriesUsageView> topSeries;

    public HallStatisticsView(Long hallId,
                              String hallName,
                              long totalBookings,
                              long cancelledBookings,
                              long totalParticipants,
                              double utilizationPercent,
                              List<SeriesUsageView> topSeries) {
        this.hallId = hallId;
        this.hallName = hallName;
        this.totalBookings = totalBookings;
        this.cancelledBookings = cancelledBookings;
        this.totalParticipants = totalParticipants;
        this.utilizationPercent = utilizationPercent;
        this.topSeries = topSeries;
    }

    public Long getHallId() {
        return hallId;
    }

    public String getHallName() {
        return hallName;
    }

    public long getTotalBookings() {
        return totalBookings;
    }

    public long getCancelledBookings() {
        return cancelledBookings;
    }

    public long getTotalParticipants() {
        return totalParticipants;
    }

    public double getUtilizationPercent() {
        return utilizationPercent;
    }

    public List<SeriesUsageView> getTopSeries() {
        return topSeries;
    }
}
