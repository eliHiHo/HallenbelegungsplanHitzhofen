package de.hallenbelegung.application.domain.model;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

public class BookingSeriesRequest {

    private final Long id;
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
    private User requestingUser;
    private final Instant createdAt;
    private Instant updatedAt;

    public BookingSeriesRequest(
            Long id,
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
            User requestingUser,
            Instant createdAt,
            Instant updatedAt
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
        this.requestingUser = Objects.requireNonNull(requestingUser);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
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
            User requestingUser
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