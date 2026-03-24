package de.hallenbelegung.application.domain.model;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

public class BookingSeriesRequest {

    private final UUID id;
    private String title;
    private String description;
    private DayOfWeek weekday;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDate startDate;
    private LocalDate endDate;
    private BookingRequestStatus status;
    private String rejectionReason;
    private Hall hall;
    private User requestedBy;
    private User processedBy;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant processedAt;

    public BookingSeriesRequest(
            UUID id,
            String title,
            String description,
            DayOfWeek weekday,
            LocalTime startTime,
            LocalTime endTime,
            LocalDate startDate,
            LocalDate endDate,
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
        this.weekday = Objects.requireNonNull(weekday);
        this.startTime = Objects.requireNonNull(startTime);
        this.endTime = Objects.requireNonNull(endTime);
        this.startDate = Objects.requireNonNull(startDate);
        this.endDate = Objects.requireNonNull(endDate);
        this.status = Objects.requireNonNull(status);
        this.rejectionReason = rejectionReason;
        this.hall = Objects.requireNonNull(hall);
        this.requestedBy = Objects.requireNonNull(requestedBy);
        this.processedBy = processedBy;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        this.processedAt = processedAt;
    }

    public static BookingSeriesRequest createNew(
            String title,
            String description,
            DayOfWeek weekday,
            LocalTime startTime,
            LocalTime endTime,
            LocalDate startDate,
            LocalDate endDate,
            Hall hall,
            User requestedBy
    ) {
        Instant now = Instant.now();

        return new BookingSeriesRequest(
                null,
                title,
                description,
                weekday,
                startTime,
                endTime,
                startDate,
                endDate,
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

    public DayOfWeek getWeekday() {
        return weekday;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
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