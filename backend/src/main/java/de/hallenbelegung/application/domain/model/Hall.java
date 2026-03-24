package de.hallenbelegung.application.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Hall {

    private final UUID id;
    private String name;
    private String description;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;
    private HallType hallType;

    public Hall(
            UUID id,
            String name,
            String description,
            boolean active,
            Instant createdAt,
            Instant updatedAt,
            HallType hallType
    ) {
        this.id = id;
        this.name = Objects.requireNonNull(name);
        this.description = description;
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        this.hallType = Objects.requireNonNull(hallType);
    }

    public static Hall createNew(String name, String description, HallType hallType) {
        Instant now = Instant.now();

        return new Hall(
                null,
                name,
                description,
                true,
                now,
                now,
                hallType
        );
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateDetails(String name, String description) {
        this.name = Objects.requireNonNull(name);
        this.description = description;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.active = true;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }

    public HallType getHallType() {
        return hallType;
    }

    public void setHallType(HallType hallType) {
        this.hallType = Objects.requireNonNull(hallType);
        this.updatedAt = Instant.now();
    }

    public boolean isFullHall() {
        return this.hallType == HallType.FULL;
    }

}