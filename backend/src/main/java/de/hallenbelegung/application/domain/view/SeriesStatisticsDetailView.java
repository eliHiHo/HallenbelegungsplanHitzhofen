package de.hallenbelegung.application.domain.view;

import java.util.List;
import java.util.UUID;

public class SeriesStatisticsDetailView {

    private final UUID bookingSeriesId;
    private final String title;
    private final String hallName;
    private final String responsibleUserName;
    private final long totalAppointments;
    private final long conductedAppointments;
    private final long cancelledAppointments;
    private final long totalParticipants;
    private final double averageParticipants;
    private final List<SeriesOccurrenceStatisticsView> occurrences;

    public SeriesStatisticsDetailView(UUID bookingSeriesId,
                                      String title,
                                      String hallName,
                                      String responsibleUserName,
                                      long totalAppointments,
                                      long conductedAppointments,
                                      long cancelledAppointments,
                                      long totalParticipants,
                                      double averageParticipants,
                                      List<SeriesOccurrenceStatisticsView> occurrences) {
        this.bookingSeriesId = bookingSeriesId;
        this.title = title;
        this.hallName = hallName;
        this.responsibleUserName = responsibleUserName;
        this.totalAppointments = totalAppointments;
        this.conductedAppointments = conductedAppointments;
        this.cancelledAppointments = cancelledAppointments;
        this.totalParticipants = totalParticipants;
        this.averageParticipants = averageParticipants;
        this.occurrences = occurrences;
    }

    public UUID getBookingSeriesId() {
        return bookingSeriesId;
    }

    public String getTitle() {
        return title;
    }

    public String getHallName() {
        return hallName;
    }

    public String getResponsibleUserName() {
        return responsibleUserName;
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

    public List<SeriesOccurrenceStatisticsView> getOccurrences() {
        return occurrences;
    }
}