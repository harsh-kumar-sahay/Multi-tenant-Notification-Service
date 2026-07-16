package com.notifsvc.notification;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenBucketTest {

    @Test
    void allowsUpToCapacityThenRejects() {
        TokenBucket bucket = new TokenBucket(3);

        assertThat(bucket.tryAcquire()).isTrue();
        assertThat(bucket.tryAcquire()).isTrue();
        assertThat(bucket.tryAcquire()).isTrue();
        assertThat(bucket.tryAcquire()).isFalse();
    }

    @Test
    void refillsOverTime() throws InterruptedException {
        // 60 tokens/minute = 1 token/second
        TokenBucket bucket = new TokenBucket(60);
        assertThat(bucket.tryAcquire()).isTrue();

        // bucket starts full at capacity minus the one just consumed; draining the rest
        for (int i = 0; i < 59; i++) {
            bucket.tryAcquire();
        }
        assertThat(bucket.tryAcquire()).isFalse();

        Thread.sleep(1100);

        assertThat(bucket.tryAcquire()).isTrue();
    }

    @Test
    void minimumCapacityIsOneEvenForZeroOrNegativeConfig() {
        TokenBucket bucket = new TokenBucket(0);

        assertThat(bucket.tryAcquire()).isTrue();
        assertThat(bucket.tryAcquire()).isFalse();
    }
}
