package com.notifsvc.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

/** Exponential backoff with jitter, capped, so a burst of failures doesn't retry in lockstep. */
@Component
public class BackoffCalculator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final long baseDelaySeconds;
    private final long maxDelaySeconds;

    public BackoffCalculator(
            @Value("${notifsvc.retry.base-delay-seconds:2}") long baseDelaySeconds,
            @Value("${notifsvc.retry.max-delay-seconds:300}") long maxDelaySeconds) {
        this.baseDelaySeconds = baseDelaySeconds;
        this.maxDelaySeconds = maxDelaySeconds;
    }

    public Instant nextAttemptAt(int attemptCount) {
        long exponential = baseDelaySeconds * (1L << Math.min(attemptCount - 1, 20));
        long capped = Math.min(exponential, maxDelaySeconds);
        long jitterMillis = (long) (capped * 1000 * 0.2 * RANDOM.nextDouble());
        return Instant.now().plus(Duration.ofSeconds(capped)).plusMillis(jitterMillis);
    }
}
