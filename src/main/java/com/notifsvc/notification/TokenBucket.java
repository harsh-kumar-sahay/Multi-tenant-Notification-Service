package com.notifsvc.notification;

/**
 * Refills continuously based on elapsed time rather than on a fixed-tick scheduler,
 * so it stays accurate regardless of how often tryAcquire is called.
 */
class TokenBucket {

    private final double capacity;
    private final double refillPerNano;
    private double availableTokens;
    private long lastRefillNanos;

    TokenBucket(int maxPerMinute) {
        this.capacity = Math.max(1, maxPerMinute);
        this.refillPerNano = capacity / (60_000_000_000.0);
        this.availableTokens = capacity;
        this.lastRefillNanos = System.nanoTime();
    }

    synchronized boolean tryAcquire() {
        refill();
        if (availableTokens >= 1.0) {
            availableTokens -= 1.0;
            return true;
        }
        return false;
    }

    private void refill() {
        long now = System.nanoTime();
        long elapsed = now - lastRefillNanos;
        if (elapsed <= 0) {
            return;
        }
        availableTokens = Math.min(capacity, availableTokens + elapsed * refillPerNano);
        lastRefillNanos = now;
    }
}
