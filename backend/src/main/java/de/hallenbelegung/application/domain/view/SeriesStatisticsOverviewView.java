package de.hallenbelegung.application.domain.view;

public class SeriesStatisticsOverviewView {

    private final Long bookingSeriesId;
    private final String title;
    private final String hallName;
    private final long totalAppointments;
    private final long conductedAppointments;
    private final long cancelledAppointments;
    private final long totalParticipants;
    private final double averageParticipants;

    public SeriesStatisticsOverviewView(Long bookingSeriesId,
                                        String title,
                                        String hallName,
                                        long totalAppointments,
                                        long conductedAppointments,
                                        long cancelledAppointments,
                                        long totalParticipants,
                                        double averageParticipants) {
        this.bookingSeriesId = bookingSeriesId;
        this.title = title;
        this.hallName = hallName;
        this.totalAppointments = totalAppointments;
        this.conductedAppointments = conductedAppointments;
        this.cancelledAppointments = cancelledAppointments;
        this.totalParticipants = totalParticipants;
        this.averageParticipants = averageParticipants;
    }

    public Long getBookingSeriesId() {
        return bookingSeriesId;
    }

    public String getTitle() {
        return title;
    }

    public String getHallName() {
        return hallName;
    }

    public long getTotalAppointments() {
        return totalAppointments;
    }

    public long getConductedAppointments() {
        return conductedAppointments;
    }

    public long getCancelledAppointments() {
        return cancelledAppointments;
    }

    public long getTotalParticipants() {
        return totalParticipants;
    }

    public double getAverageParticipants() {
        return averageParticipants;
    }
}