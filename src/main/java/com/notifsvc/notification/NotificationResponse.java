package com.notifsvc.notification;

import com.notifsvc.channel.ChannelType;

import java.time.Instant;
import java.util.Map;

public record NotificationResponse(
        Long id,
        Long templateId,
        ChannelType channelType,
        String recipient,
        Map<String, String> variables,
        NotificationStatus status,
        Instant scheduledAt,
        Instant nextAttemptAt,
        int attemptCount,
        int maxAttempts,
        String idempotencyKey,
        Instant createdAt,
        Instant updatedAt
) {
    public static NotificationResponse from(NotificationRequest n) {
        return new NotificationResponse(
                n.getId(),
                n.getTemplate().getId(),
                n.getChannelType(),
                n.getRecipient(),
                n.getVariables(),
                n.getStatus(),
                n.getScheduledAt(),
                n.getNextAttemptAt(),
                n.getAttemptCount(),
                n.getMaxAttempts(),
                n.getIdempotencyKey(),
                n.getCreatedAt(),
                n.getUpdatedAt());
    }
}
