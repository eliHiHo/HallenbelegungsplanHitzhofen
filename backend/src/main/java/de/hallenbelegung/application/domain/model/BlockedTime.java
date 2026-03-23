package de.hallenbelegung.application.domain.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;

public class BlockedTime {

    private final Long id;
    private String reason;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private Hall hall;
    private final Instant createdAt;
    private Instant updatedAt;

    public BlockedTime(
            Long id,
            String reason,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            Hall hall,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.reason = Objects.requireNonNull(reason);
        this.startDateTime = Objects.requireNonNull(startDateTime);
        this.endDateTime = Objects.requireNonNull(endDateTime);
        this.hall = Objects.requireNonNull(hall);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static BlockedTime createNew(
            String reason,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            Hall hall
    ) {
        Instant now = Instant.now();

        return new BlockedTime(
                null,
                reason,
                startDateTime,
                endDateTime,
                hall,
                now,
                now
        );
    }

    public Long getId() {
        return id;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public Hall getHall() {
        return hall;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateDetails(
            String reason,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            Hall hall
    ) {
        this.reason = Objects.requireNonNull(reason);
        this.startDateTime = Objects.requireNonNull(startDateTime);
        this.endDateTime = Objects.requireNonNull(endDateTime);
        this.hall = Objects.requireNonNull(hall);
        this.updatedAt = Instant.now();
    }
}