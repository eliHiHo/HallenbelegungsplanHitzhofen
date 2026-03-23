package de.hallenbelegung.application.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public class BookingRequest {

    private final Long id;
    private String title;
    private String description;
    private LocalDate date;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private BookingRequestStatus status;
    private String rejectionReason;
    private Hall hall;
    private User requestingUser;
    private final Instant createdAt;
    private Instant updatedAt;

    public BookingRequest(
            Long id,
            String title,
            String description,
            LocalDate date,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            BookingRequestStatus status,
            String rejectionReason,
            Hall hall,
            User requestingUser,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.title = Objects.requireNonNull(title);
        this.description = description;
        this.date = Objects.requireNonNull(date);
        this.startDateTime = Objects.requireNonNull(startDateTime);
        this.endDateTime = Objects.requireNonNull(endDateTime);
        this.status = Objects.requireNonNull(status);
        this.rejectionReason = rejectionReason;
        this.hall = Objects.requireNonNull(hall);
        this.requestingUser = Objects.requireNonNull(requestingUser);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static BookingRequest createNew(
            String title,
            String description,
            LocalDate date,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            Hall hall,
            User requestingUser
    ) {
        Instant now = Instant.now();

        return new BookingRequest(
                null,
                title,
                description,
                date,
                startDateTime,
                endDateTime,
                BookingRequestStatus.OPEN,
                null,
                hall,
                requestingUser,
                now,
                now
        );
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
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

    public BookingRequestStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public Hall getHall() {
        return hall;
    }

    public User getRequestingUser() {
        return requestingUser;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isOpen() {
        return status == BookingRequestStatus.OPEN;
    }

    public boolean isApproved() {
        return status == BookingRequestStatus.APPROVED;
    }

    public boolean isRejected() {
        return status == BookingRequestStatus.REJECTED;
    }

    public void approve() {
        this.status = BookingRequestStatus.APPROVED;
        this.rejectionReason = null;
        this.updatedAt = Instant.now();
    }

    public void reject(String rejectionReason) {
        this.status = BookingRequestStatus.REJECTED;
        this.rejectionReason = rejectionReason;
        this.updatedAt = Instant.now();
    }
}