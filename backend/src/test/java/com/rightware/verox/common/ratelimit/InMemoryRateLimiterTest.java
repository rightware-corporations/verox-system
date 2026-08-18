package com.rightware.verox.common.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRateLimiterTest {

    @Test
    void rejectsAfterConfiguredLimit() {
        MutableClock clock =
            new MutableClock(Instant.parse("2026-08-14T10:00:00Z"));

        InMemoryRateLimiter limiter =
            new InMemoryRateLimiter(100, clock);

        assertThat(limiter.consume("checkout:1", 2, Duration.ofMinutes(1)).allowed())
            .isTrue();

        assertThat(limiter.consume("checkout:1", 2, Duration.ofMinutes(1)).allowed())
            .isTrue();

        RateLimitDecision rejected =
            limiter.consume("checkout:1", 2, Duration.ofMinutes(1));

        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.remaining()).isZero();
        assertThat(rejected.retryAfterSeconds()).isEqualTo(60);
    }

    @Test
    void resetsAfterWindowExpires() {
        MutableClock clock =
            new MutableClock(Instant.parse("2026-08-14T10:00:00Z"));

        InMemoryRateLimiter limiter =
            new InMemoryRateLimiter(100, clock);

        assertThat(limiter.consume("checkout:1", 1, Duration.ofSeconds(30)).allowed())
            .isTrue();

        assertThat(limiter.consume("checkout:1", 1, Duration.ofSeconds(30)).allowed())
            .isFalse();

        clock.advance(Duration.ofSeconds(31));

        RateLimitDecision decision =
            limiter.consume("checkout:1", 1, Duration.ofSeconds(30));

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.remaining()).isZero();
    }

    @Test
    void keepsDifferentSecurityIdentitiesIndependent() {
        MutableClock clock =
            new MutableClock(Instant.parse("2026-08-14T10:00:00Z"));

        InMemoryRateLimiter limiter =
            new InMemoryRateLimiter(100, clock);

        assertThat(limiter.consume("merchant:key-a", 1, Duration.ofMinutes(1)).allowed())
            .isTrue();

        assertThat(limiter.consume("merchant:key-a", 1, Duration.ofMinutes(1)).allowed())
            .isFalse();

        assertThat(limiter.consume("merchant:key-b", 1, Duration.ofMinutes(1)).allowed())
            .isTrue();
    }

    @Test
    void boundsStoredBucketCount() {
        MutableClock clock =
            new MutableClock(Instant.parse("2026-08-14T10:00:00Z"));

        InMemoryRateLimiter limiter =
            new InMemoryRateLimiter(3, clock);

        limiter.consume("a", 10, Duration.ofMinutes(1));
        limiter.consume("b", 10, Duration.ofMinutes(1));
        limiter.consume("c", 10, Duration.ofMinutes(1));
        limiter.consume("d", 10, Duration.ofMinutes(1));
        limiter.consume("e", 10, Duration.ofMinutes(1));

        assertThat(limiter.bucketCount()).isEqualTo(3);
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}