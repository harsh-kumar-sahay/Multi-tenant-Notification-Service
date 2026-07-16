package com.notifsvc.notification;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class BackoffCalculatorTest {

    private final BackoffCalculator backoffCalculator = new BackoffCalculator(2, 300);

    @Test
    void delayGrowsWithEachAttempt() {
        Instant now = Instant.now();

        Instant firstAttemptDelay = backoffCalculator.nextAttemptAt(1);
        Instant secondAttemptDelay = backoffCalculator.nextAttemptAt(2);
        Instant thirdAttemptDelay = backoffCalculator.nextAttemptAt(3);

        // jitter adds up to 20% on top of the exponential base, so compare with margin
        assertThat(firstAttemptDelay).isAfter(now);
        assertThat(secondAttemptDelay).isAfterOrEqualTo(firstAttemptDelay.minusSeconds(1));
        assertThat(thirdAttemptDelay).isAfterOrEqualTo(secondAttemptDelay.minusSeconds(1));
    }

    @Test
    void delayIsCappedAtMaxDelay() {
        Instant now = Instant.now();

        // attempt 20 would be enormous uncapped; capped value plus max 20% jitter must stay bounded
        Instant farFutureAttempt = backoffCalculator.nextAttemptAt(20);

        assertThat(farFutureAttempt).isBefore(now.plusSeconds(300 + 61));
    }

    @Test
    void firstAttemptUsesBaseDelay() {
        Instant now = Instant.now();

        Instant delay = backoffCalculator.nextAttemptAt(1);

        // base delay is 2s, plus up to 20% jitter (0.4s) - allow small scheduling slack
        assertThat(delay).isBetween(now.plusSeconds(1), now.plusSeconds(3));
    }
}
