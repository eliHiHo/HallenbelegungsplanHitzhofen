package de.hallenbelegung.application.domain.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class BlockedTime {

    private final UUID id;
    private String reason;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private BlockedTimeType type;
    private Hall hall;
    private User createdBy;
    private User updatedBy;
    private final Instant createdAt;
    private Instant updatedAt;

    public BlockedTime(
            UUID id,
            String reason,
            LocalDateTime startAt,
            LocalDateTime endAt,
            BlockedTimeType type,
            Hall hall,
            User createdBy,
            User updatedBy,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.reason = Objects.requireNonNull(reason);
        this.startAt = Objects.requireNonNull(startAt);
        this.endAt = Objects.requireNonNull(endAt);
        this.type = Objects.requireNonNull(type);
        this.hall = Objects.requireNonNull(hall);
        this.createdBy = Objects.requireNonNull(createdBy);
        this.updatedBy = updatedBy;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static BlockedTime createNew(
            String reason,
            LocalDateTime startAt,
            LocalDateTime endAt,
            BlockedTimeType type,
            Hall hall,
            User createdBy
    ) {
        Instant now = Instant.now();

        return new BlockedTime(
                null,
                reason,
                startAt,
                endAt,
                type,
                hall,
                createdBy,
                createdBy,
                now,
                now
        );
    }

    public UUID getId() {
        return id;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public BlockedTimeType getType() {
        return type;
    }

    public Hall getHall() {
        return hall;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public User getUpdatedBy() {
        return updatedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateDetails(
            String reason,
            LocalDateTime startAt,
            LocalDateTime endAt,
            BlockedTimeType type,
            Hall hall,
            User updatedBy
    ) {
        this.reason = Objects.requireNonNull(reason);
        this.startAt = Objects.requireNonNull(startAt);
        this.endAt = Objects.requireNonNull(endAt);
        this.type = Objects.requireNonNull(type);
        this.hall = Objects.requireNonNull(hall);
        this.updatedBy = Objects.requireNonNull(updatedBy);
        this.updatedAt = Instant.now();
    }
}