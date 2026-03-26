package de.hallenbelegung.adapters.out.persistance.entity;

import de.hallenbelegung.application.domain.model.BookingStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "bookings",
        indexes = {
                @Index(name = "idx_bookings_hall_start_end", columnList = "hall_id,start_at,end_at"),
                @Index(name = "idx_bookings_responsible_user", columnList = "responsible_user_id"),
                @Index(name = "idx_bookings_series", columnList = "booking_series_id"),
                @Index(name = "idx_bookings_status", columnList = "status"),
                @Index(name = "idx_bookings_start_at", columnList = "start_at")
        }
)
public class DBBooking {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private BookingStatus status;

    @Column(name = "participant_count")
    private Integer participantCount;

    @Column(name = "conducted", nullable = false)
    private boolean conducted = false;

    @Column(name = "feedback_comment", columnDefinition = "TEXT")
    private String feedbackComment;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hall_id", nullable = false)
    private DBHall hall;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsible_user_id", nullable = false)
    private DBUser responsibleUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_series_id")
    private DBBookingSeries bookingSeries;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private DBUser createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id")
    private DBUser updatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by_user_id")
    private DBUser cancelledBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    public DBBooking() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public Integer getParticipantCount() {
        return participantCount;
    }

    public void setParticipantCount(Integer participantCount) {
        this.participantCount = participantCount;
    }

    public boolean getConducted() {
        return conducted;
    }

    public void setConducted(Boolean conducted) {
        this.conducted = (conducted != null) ? conducted : false;
    }

    public String getFeedbackComment() {
        return feedbackComment;
    }

    public void setFeedbackComment(String feedbackComment) {
        this.feedbackComment = feedbackComment;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public DBHall getHall() {
        return hall;
    }

    public void setHall(DBHall hall) {
        this.hall = hall;
    }

    public DBUser getResponsibleUser() {
        return responsibleUser;
    }

    public void setResponsibleUser(DBUser responsibleUser) {
        this.responsibleUser = responsibleUser;
    }

    public DBBookingSeries getBookingSeries() {
        return bookingSeries;
    }

    public void setBookingSeries(DBBookingSeries bookingSeries) {
        this.bookingSeries = bookingSeries;
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

    public DBUser getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(DBUser cancelledBy) {
        this.cancelledBy = cancelledBy;
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

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }
}