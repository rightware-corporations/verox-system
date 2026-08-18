package com.rightware.verox.common.ratelimit;

public record RateLimitDecision(
    boolean allowed,
    int remaining,
    long retryAfterSeconds
) {
}