package de.hallenbelegung.application.domain.model;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

public class BookingSeries {

    private final Long id;
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
    private final Instant createdAt;
    private Instant updatedAt;
    private String cancellationReason;

    public BookingSeries(
            Long id,
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
            Instant createdAt,
            Instant updatedAt,
            String cancellationReason
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
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        this.cancellationReason = cancellationReason;
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
                now,
                now,
                null
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

    public BookingSeriesStatus getStatus() {
        return status;
    }

    public Hall getHall() {
        return hall;
    }

    public User getResponsibleUser() {
        return responsibleUser;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isCancelled() {
        return status == BookingSeriesStatus.CANCELLED;
    }

    public void cancel(String cancellationReason) {
        this.cancellationReason = cancellationReason;
        this.status = BookingSeriesStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }
}