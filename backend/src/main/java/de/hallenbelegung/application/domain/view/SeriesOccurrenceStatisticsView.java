package de.hallenbelegung.application.domain.view;

import java.time.LocalDateTime;
import java.util.UUID;

public class SeriesOccurrenceStatisticsView {

    private final UUID bookingId;
    private final LocalDateTime startDateTime;
    private final LocalDateTime endDateTime;
    private final boolean cancelled;
    private final boolean conducted;
    private final Integer participantCount;
    private final String feedbackComment;

    public SeriesOccurrenceStatisticsView(UUID bookingId,
                                          LocalDateTime startDateTime,
                                          LocalDateTime endDateTime,
                                          boolean cancelled,
                                          boolean conducted,
                                          Integer participantCount,
                                          String feedbackComment) {
        this.bookingId = bookingId;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.cancelled = cancelled;
        this.conducted = conducted;
        this.participantCount = participantCount;
        this.feedbackComment = feedbackComment;
    }

    public UUID getBookingId() {
        return bookingId;
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