package com.notifsvc.support;

import java.time.Duration;
import java.time.Instant;
import java.util.function.BooleanSupplier;

/** Polls a condition until it's true or a timeout elapses - used to wait on the async dispatch poller in tests. */
public final class Awaiter {

    private Awaiter() {
    }

    public static void await(Duration timeout, BooleanSupplier condition) {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        throw new AssertionError("Condition not met within " + timeout);
    }
}
