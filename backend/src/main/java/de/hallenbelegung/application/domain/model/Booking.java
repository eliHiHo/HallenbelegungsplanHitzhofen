package de.hallenbelegung.application.domain.model;

import java.time.Instant;
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
    private Hall hall;
    private User responsibleUser;
    private BookingSeries bookingSeries;
    private final Instant createdAt;
    private Instant updatedAt;
    private User createdBy;
    private User updatedBy;
    private User cancelledBy;
    private Instant cancelledAt;
    private String cancelReason;

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
            Hall hall,
            User responsibleUser,
            BookingSeries bookingSeries,
            Instant createdAt,
            Instant updatedAt,
            User createdBy,
            User updatedBy,
            User cancelledBy,
            Instant cancelledAt,
            String cancelReason
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
        this.hall = Objects.requireNonNull(hall);
        this.responsibleUser = Objects.requireNonNull(responsibleUser);
        this.bookingSeries = bookingSeries;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.cancelledBy = cancelledBy;
        this.cancelledAt = cancelledAt;
        this.cancelReason = cancelReason;
    }

    public static Booking createNew(
            String title,
            String description,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Hall hall,
            User responsibleUser,
            BookingSeries bookingSeries,
            User createdBy,
            User updatedBy,
            User cancelledBy,
            Instant cancelledAt,
            String cancelReason
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
                hall,
                responsibleUser,
                bookingSeries,
                now,
                now,
                createdBy,
                updatedBy,
                cancelledBy,
                cancelledAt,
                cancelReason
        );
    }

    // Convenience overload used by services: minimal creation with common fields
    public static Booking createNew(
            String title,
            String description,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Hall hall,
            User responsibleUser,
            BookingSeries bookingSeries
    ) {
        return createNew(title, description, startAt, endAt, hall, responsibleUser, bookingSeries, null, null, null, null, null);
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

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
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

    public User getCreatedBy() {
        return createdBy;
    }
    public User getUpdatedBy() {
        return updatedBy;
    }
    public User getCancelledBy() {
        return cancelledBy;
    }
    public Instant getCancelledAt() {
        return cancelledAt;
    }
    public String getCancelReason() {
        return cancelReason;
    }

    public void updateDetails(
            String title,
            String description,
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

    public void cancel(User cancelledBy, String cancellationReason) {
        this.status = BookingStatus.CANCELLED;
        this.cancelReason = cancellationReason;
        Instant now = Instant.now();
        this.updatedAt = now;
        this.cancelledAt = now;
        this.cancelledBy = Objects.requireNonNull(cancelledBy);
        this.updatedBy = cancelledBy;
    }

    public void addFeedback(Integer participantCount, String feedbackComment) {
        this.participantCount = participantCount;
        this.feedbackComment = feedbackComment;
        this.conducted = true;
        this.updatedAt = Instant.now();
    }
}