package de.hallenbelegung.adapters.out.persistance.entity;

import de.hallenbelegung.application.domain.model.BlockedTimeType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "blocked_times",
        indexes = {
                @Index(name = "idx_blocked_times_hall_time", columnList = "hall_id,start_at,end_at"),
                @Index(name = "idx_blocked_times_type", columnList = "type")
        }
)
public class DBBlockedTime {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "reason", nullable = false, length = 255)
    private String reason;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private BlockedTimeType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hall_id", nullable = false)
    private DBHall hall;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private DBUser createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id")
    private DBUser updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public DBBlockedTime() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public BlockedTimeType getType() {
        return type;
    }

    public void setType(BlockedTimeType type) {
        this.type = type;
    }

    public DBHall getHall() {
        return hall;
    }

    public void setHall(DBHall hall) {
        this.hall = hall;
    }

    public DBUser getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(DBUser createdBy) {
        this.createdBy = createdBy;
    }

    public DBUser getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(DBUser updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}