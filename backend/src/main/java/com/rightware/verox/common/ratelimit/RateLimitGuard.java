package com.rightware.verox.common.ratelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

@Component
public class RateLimitGuard {

    private static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";

    private final InMemoryRateLimiter rateLimiter;

    private final int checkoutSubmissionLimit;
    private final Duration checkoutSubmissionWindow;

    private final int merchantApiLimit;
    private final Duration merchantApiWindow;

    private final int bridgeLimit;
    private final Duration bridgeWindow;

    public RateLimitGuard(
        InMemoryRateLimiter rateLimiter,

        @Value("${verox.rate-limit.checkout-submission.limit:6}")
        int checkoutSubmissionLimit,

        @Value("${verox.rate-limit.checkout-submission.window-seconds:60}")
        long checkoutSubmissionWindowSeconds,

        @Value("${verox.rate-limit.merchant-api.limit:120}")
        int merchantApiLimit,

        @Value("${verox.rate-limit.merchant-api.window-seconds:60}")
        long merchantApiWindowSeconds,

        @Value("${verox.rate-limit.bridge.limit:60}")
        int bridgeLimit,

        @Value("${verox.rate-limit.bridge.window-seconds:60}")
        long bridgeWindowSeconds
    ) {
        this.rateLimiter = Objects.requireNonNull(rateLimiter, "rateLimiter");

        requirePositive(checkoutSubmissionLimit, "checkoutSubmissionLimit");
        requirePositive(checkoutSubmissionWindowSeconds, "checkoutSubmissionWindowSeconds");
        requirePositive(merchantApiLimit, "merchantApiLimit");
        requirePositive(merchantApiWindowSeconds, "merchantApiWindowSeconds");
        requirePositive(bridgeLimit, "bridgeLimit");
        requirePositive(bridgeWindowSeconds, "bridgeWindowSeconds");

        this.checkoutSubmissionLimit = checkoutSubmissionLimit;
        this.checkoutSubmissionWindow = Duration.ofSeconds(checkoutSubmissionWindowSeconds);

        this.merchantApiLimit = merchantApiLimit;
        this.merchantApiWindow = Duration.ofSeconds(merchantApiWindowSeconds);

        this.bridgeLimit = bridgeLimit;
        this.bridgeWindow = Duration.ofSeconds(bridgeWindowSeconds);
    }

    public void checkCheckoutSubmission(UUID checkoutSessionId) {
        Objects.requireNonNull(checkoutSessionId, "checkoutSessionId");

        enforce(
            "checkout:" + checkoutSessionId,
            checkoutSubmissionLimit,
            checkoutSubmissionWindow
        );
    }

    public void checkMerchantApi(UUID apiKeyId) {
        Objects.requireNonNull(apiKeyId, "apiKeyId");

        enforce(
            "merchant-api:" + apiKeyId,
            merchantApiLimit,
            merchantApiWindow
        );
    }

    public void checkBridge(UUID bridgeId) {
        Objects.requireNonNull(bridgeId, "bridgeId");

        enforce(
            "bridge:" + bridgeId,
            bridgeLimit,
            bridgeWindow
        );
    }

    private void enforce(String key, int limit, Duration window) {
        RateLimitDecision decision =
            rateLimiter.consume(key, limit, window);

        if (!decision.allowed()) {
            throw new RateLimitExceededException(
                RATE_LIMIT_EXCEEDED,
                "Too many requests. Try again later.",
                decision.retryAfterSeconds()
            );
        }
    }

    private static void requirePositive(long value, String name) {
        if (value < 1) {
            throw new IllegalArgumentException(
                name + " must be positive"
            );
        }
    }
}