package com.notifsvc.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.Map;

public record NotificationCreateRequest(
        @NotNull Long templateId,
        @NotBlank String recipient,
        Map<String, String> variables,
        /** Null means send immediately. */
        Instant scheduledAt,
        @NotBlank String idempotencyKey,
        @Positive Integer maxAttempts
) {
}
