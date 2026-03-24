package de.hallenbelegung.application.domain.model;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

public class BookingSeries {

    private final UUID id;
    private String title;
    private String description;
    private DayOfWeek weekday;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDate startDate;
    private LocalDate endDate;
    private BookingSeriesStatus status;
    private Hall hall;
    private User responsibleUser;
    private final User createdBy;
    private User updatedBy;
    private User cancelledBy;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant cancelledAt;
    private String cancelReason;

    public BookingSeries(
            UUID id,
            String title,
            String description,
            DayOfWeek weekday,
            LocalTime startTime,
            LocalTime endTime,
            LocalDate startDate,
            LocalDate endDate,
            BookingSeriesStatus status,
            Hall hall,
            User responsibleUser,
            User createdBy,
            User updatedBy,
            User cancelledBy,
            Instant createdAt,
            Instant updatedAt,
            Instant cancelledAt,
            String cancelReason
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
        this.hall = Objects.requireNonNull(hall);
        this.responsibleUser = Objects.requireNonNull(responsibleUser);
        this.createdBy = Objects.requireNonNull(createdBy);
        this.updatedBy = Objects.requireNonNull(updatedBy);
        this.cancelledBy = cancelledBy;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        this.cancelledAt = cancelledAt;
        this.cancelReason = cancelReason;
    }

    public static BookingSeries createNew(
            String title,
            String description,
            DayOfWeek weekday,
            LocalTime startTime,
            LocalTime endTime,
            LocalDate startDate,
            LocalDate endDate,
            Hall hall,
            User responsibleUser
    ) {
        Instant now = Instant.now();

        // Use responsibleUser as creator/updater initially
        return new BookingSeries(
                null,
                title,
                description,
                weekday,
                startTime,
                endTime,
                startDate,
                endDate,
                BookingSeriesStatus.ACTIVE,
                hall,
                responsibleUser,
                responsibleUser, // createdBy
                responsibleUser, // updatedBy
                null,
                now,
                now,
                null,
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

    public BookingSeriesStatus getStatus() {
        return status;
    }

    public Hall getHall() {
        return hall;
    }

    public User getResponsibleUser() {
        return responsibleUser;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public boolean isCancelled() {
        return status == BookingSeriesStatus.CANCELLED;
    }

    public void cancel(User cancelledBy, String cancelReason) {
        Instant now = Instant.now();
        this.cancelledBy = Objects.requireNonNull(cancelledBy);
        this.cancelReason = cancelReason;
        this.cancelledAt = now;
        this.updatedAt = now;
        this.updatedBy = cancelledBy;
        this.status = BookingSeriesStatus.CANCELLED;
    }

    public void updateMetadata(String title, String description, User updatedBy) {
        this.title = Objects.requireNonNull(title);
        this.description = description;
        this.updatedBy = Objects.requireNonNull(updatedBy);
        this.updatedAt = Instant.now();
    }
}