package com.rightware.verox.common.ratelimit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class InMemoryRateLimiter {

    private final int maxBuckets;
    private final Clock clock;

    /*
     * accessOrder=true gives us bounded LRU-style eviction.
     * All access is guarded by consume() synchronization.
     */
    private final LinkedHashMap<String, Bucket> buckets =
        new LinkedHashMap<>(16, 0.75f, true);

    @Autowired
    public InMemoryRateLimiter(
        @Value("${verox.rate-limit.max-buckets:10000}") int maxBuckets
    ) {
        this(maxBuckets, Clock.systemUTC());
    }

    InMemoryRateLimiter(int maxBuckets, Clock clock) {
        if (maxBuckets < 1) {
            throw new IllegalArgumentException("maxBuckets must be positive");
        }
        this.maxBuckets = maxBuckets;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public synchronized RateLimitDecision consume(
        String key,
        int limit,
        Duration window
    ) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Rate-limit key is required");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("Rate limit must be positive");
        }
        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Rate-limit window must be positive");
        }

        Instant now = clock.instant();
        Bucket bucket = buckets.get(key);

        if (bucket == null || !now.isBefore(bucket.windowEndsAt())) {
            ensureCapacityForNewBucket(key);

            Instant windowEndsAt = now.plus(window);
            buckets.put(key, new Bucket(1, windowEndsAt));

            return new RateLimitDecision(
                true,
                limit - 1,
                0
            );
        }

        if (bucket.count() >= limit) {
            return new RateLimitDecision(
                false,
                0,
                retryAfterSeconds(now, bucket.windowEndsAt())
            );
        }

        int nextCount = bucket.count() + 1;

        buckets.put(
            key,
            new Bucket(nextCount, bucket.windowEndsAt())
        );

        return new RateLimitDecision(
            true,
            limit - nextCount,
            0
        );
    }

    synchronized int bucketCount() {
        return buckets.size();
    }

    private void ensureCapacityForNewBucket(String key) {
        if (buckets.containsKey(key) || buckets.size() < maxBuckets) {
            return;
        }

        Iterator<Map.Entry<String, Bucket>> iterator =
            buckets.entrySet().iterator();

        if (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    private long retryAfterSeconds(
        Instant now,
        Instant windowEndsAt
    ) {
        long millis =
            Duration.between(now, windowEndsAt).toMillis();

        if (millis <= 0) {
            return 1;
        }

        return Math.max(1, (millis + 999) / 1000);
    }

    private record Bucket(
        int count,
        Instant windowEndsAt
    ) {
    }
}