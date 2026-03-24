package de.hallenbelegung.application.domain.view;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class SeriesOccurrenceStatisticsView {

    private final Long bookingId;
    private final LocalDate date;
    private final LocalDateTime startDateTime;
    private final LocalDateTime endDateTime;
    private final boolean cancelled;
    private final boolean conducted;
    private final Integer participantCount;
    private final String feedbackComment;

    public SeriesOccurrenceStatisticsView(Long bookingId,
                                          LocalDate date,
                                          LocalDateTime startDateTime,
                                          LocalDateTime endDateTime,
                                          boolean cancelled,
                                          boolean conducted,
                                          Integer participantCount,
                                          String feedbackComment) {
        this.bookingId = bookingId;
        this.date = date;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.cancelled = cancelled;
        this.conducted = conducted;
        this.participantCount = participantCount;
        this.feedbackComment = feedbackComment;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public boolean isConducted() {
        return conducted;
    }

    public Integer getParticipantCount() {
        return participantCount;
    }

    public String getFeedbackComment() {
        return feedbackComment;
    }
}