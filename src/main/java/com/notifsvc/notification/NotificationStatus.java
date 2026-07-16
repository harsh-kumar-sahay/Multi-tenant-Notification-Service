package com.notifsvc.notification;

public enum NotificationStatus {
    PENDING,
    SENDING,
    SENT,
    DELIVERED,
    FAILED,
    RETRY_SCHEDULED,
    DEAD_LETTER,
    CANCELLED
}
