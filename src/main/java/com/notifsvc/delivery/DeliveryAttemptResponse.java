package com.notifsvc.delivery;

import java.time.Instant;

public record DeliveryAttemptResponse(
        Long id,
        int attemptNumber,
        AttemptStatus status,
        String errorMessage,
        String workerId,
        Instant startedAt,
        Instant completedAt
) {
    public static DeliveryAttemptResponse from(DeliveryAttempt attempt) {
        return new DeliveryAttemptResponse(
                attempt.getId(),
                attempt.getAttemptNumber(),
                attempt.getStatus(),
                attempt.getErrorMessage(),
                attempt.getWorkerId(),
                attempt.getStartedAt(),
                attempt.getCompletedAt());
    }
}
