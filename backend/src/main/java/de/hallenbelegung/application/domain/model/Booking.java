package de.hallenbelegung.application.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Booking {

    private final UUID id;
    private String title;
    private String description;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private BookingStatus status;
    private Integer participantCount;
    private boolean conducted;
    private String feedbackComment;
    private String cancellationReason;
    private Hall hall;
    private User responsibleUser;
    private BookingSeries bookingSeries;
    private final Instant createdAt;
    private Instant updatedAt;

    public Booking(
            UUID id,
            String title,
            String description,
            LocalDateTime startAt,
            LocalDateTime endAt,
            BookingStatus status,
            Integer participantCount,
            boolean conducted,
            String feedbackComment,
            String cancellationReason,
            Hall hall,
            User responsibleUser,
            BookingSeries bookingSeries,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.title = Objects.requireNonNull(title);
        this.description = description;
        this.startAt = Objects.requireNonNull(startAt);
        this.endAt = Objects.requireNonNull(endAt);
        this.status = Objects.requireNonNull(status);
        this.participantCount = participantCount;
        this.conducted = conducted;
        this.feedbackComment = feedbackComment;
        this.cancellationReason = cancellationReason;
        this.hall = Objects.requireNonNull(hall);
        this.responsibleUser = Objects.requireNonNull(responsibleUser);
        this.bookingSeries = bookingSeries;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static Booking createNew(
            String title,
            String description,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Hall hall,
            User responsibleUser,
            BookingSeries bookingSeries
    ) {
        Instant now = Instant.now();

        return new Booking(
                null,
                title,
                description,
                startAt,
                endAt,
                BookingStatus.APPROVED,
                null,
                false,
                null,
                null,
                hall,
                responsibleUser,
                bookingSeries,
                now,
                now
        );
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }


    public LocalDateTime getstartAt() {
        return startAt;
    }

    public LocalDateTime getendAt() {
        return endAt;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public Integer getParticipantCount() {
        return participantCount;
    }

    public boolean isConducted() {
        return conducted;
    }

    public String getFeedbackComment() {
        return feedbackComment;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public Hall getHall() {
        return hall;
    }

    public User getResponsibleUser() {
        return responsibleUser;
    }

    public BookingSeries getBookingSeries() {
        return bookingSeries;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean belongsToSeries() {
        return bookingSeries != null;
    }

    public boolean isCancelled() {
        return status == BookingStatus.CANCELLED;
    }

    public void updateDetails(
            String title,
            String description,
            LocalDate date,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Hall hall
    ) {
        this.title = Objects.requireNonNull(title);
        this.description = description;
        this.startAt = Objects.requireNonNull(startAt);
        this.endAt = Objects.requireNonNull(endAt);
        this.hall = Objects.requireNonNull(hall);
        this.updatedAt = Instant.now();
    }

    public void cancel(String cancellationReason) {
        this.status = BookingStatus.CANCELLED;
        this.cancellationReason = cancellationReason;
        this.updatedAt = Instant.now();
    }

    public void addFeedback(Integer participantCount, String feedbackComment) {
        this.participantCount = participantCount;
        this.feedbackComment = feedbackComment;
        this.conducted = true;
        this.updatedAt = Instant.now();
    }
}