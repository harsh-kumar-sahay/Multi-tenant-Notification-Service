package com.notifsvc.delivery;

import com.notifsvc.notification.NotificationRequest;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "delivery_attempts", indexes = {
    @Index(name = "idx_attempt_notification", columnList = "notification_id")
})
public class DeliveryAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private NotificationRequest notification;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttemptStatus status;

    private String errorMessage;

    private String workerId;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant completedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public NotificationRequest getNotification() { return notification; }
    public void setNotification(NotificationRequest notification) { this.notification = notification; }
    public int getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(int attemptNumber) { this.attemptNumber = attemptNumber; }
    public AttemptStatus getStatus() { return status; }
    public void setStatus(AttemptStatus status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
