package com.notifsvc.channel;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * No real email/SMS/push/in-app provider is in scope for this assignment.
 * Each sender simulates provider latency and a configurable transient-failure rate
 * so the retry/backoff path is actually exercisable end-to-end.
 */
public abstract class AbstractSimulatedSender implements NotificationSender {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final double failureRate;
    private final long simulatedLatencyMillis;

    protected AbstractSimulatedSender(double failureRate, long simulatedLatencyMillis) {
        this.failureRate = failureRate;
        this.simulatedLatencyMillis = simulatedLatencyMillis;
    }

    @Override
    public SendResult send(String recipient, String renderedBody) {
        if (recipient == null || recipient.isBlank()) {
            throw new IllegalArgumentException("recipient must not be blank for channel " + supportedChannel());
        }
        simulateLatency();
        if (RANDOM.nextDouble() < failureRate) {
            return SendResult.failure("Simulated transient provider failure for " + supportedChannel());
        }
        return SendResult.ok(UUID.randomUUID().toString());
    }

    private void simulateLatency() {
        if (simulatedLatencyMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(simulatedLatencyMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
