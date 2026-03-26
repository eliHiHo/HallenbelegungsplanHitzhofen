package de.hallenbelegung.application.domain.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class BookingRequest {

    private final UUID id;
    private String title;
    private String description;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private BookingRequestStatus status;
    private String rejectionReason;
    private Hall hall;
    private User requestedBy;
    private User processedBy;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant processedAt;

    public BookingRequest(
            UUID id,
            String title,
            String description,
            LocalDateTime startAt,
            LocalDateTime endAt,
            BookingRequestStatus status,
            String rejectionReason,
            Hall hall,
            User requestedBy,
            User processedBy,
            Instant createdAt,
            Instant updatedAt,
            Instant processedAt
    ) {
        this.id = id;
        this.title = Objects.requireNonNull(title);
        this.description = description;
        this.startAt = Objects.requireNonNull(startAt);
        this.endAt = Objects.requireNonNull(endAt);
        this.status = Objects.requireNonNull(status);
        this.rejectionReason = rejectionReason;
        this.hall = Objects.requireNonNull(hall);
        this.requestedBy = Objects.requireNonNull(requestedBy);
        this.processedBy = processedBy;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        this.processedAt = processedAt;
    }

    public static BookingRequest createNew(
            String title,
            String description,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Hall hall,
            User requestedBy
    ) {
        Instant now = Instant.now();

        return new BookingRequest(
                null,
                title,
                description,
                startAt,
                endAt,
                BookingRequestStatus.PENDING,
                null,
                hall,
                requestedBy,
                null,
                now,
                now,
                null
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

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public BookingRequestStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public Hall getHall() {
        return hall;
    }

    public User getRequestedBy() {
        return requestedBy;
    }

    public User getProcessedBy() {
        return processedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public boolean isPending() {
        return status == BookingRequestStatus.PENDING;
    }

    public boolean isApproved() {
        return status == BookingRequestStatus.APPROVED;
    }

    public boolean isRejected() {
        return status == BookingRequestStatus.REJECTED;
    }

    public void approve(User processedBy) {
        Instant now = Instant.now();
        this.status = BookingRequestStatus.APPROVED;
        this.rejectionReason = null;
        this.processedBy = Objects.requireNonNull(processedBy);
        this.processedAt = now;
        this.updatedAt = now;
    }

    public void reject(User processedBy, String rejectionReason) {
        Instant now = Instant.now();
        this.status = BookingRequestStatus.REJECTED;
        this.rejectionReason = rejectionReason;
        this.processedBy = Objects.requireNonNull(processedBy);
        this.processedAt = now;
        this.updatedAt = now;
    }
}