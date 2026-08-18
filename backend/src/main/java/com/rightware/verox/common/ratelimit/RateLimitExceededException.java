package com.rightware.verox.common.ratelimit;

import com.rightware.verox.common.web.ApiException;
import org.springframework.http.HttpStatus;

public class RateLimitExceededException extends ApiException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(
        String code,
        String message,
        long retryAfterSeconds
    ) {
        super(
            HttpStatus.TOO_MANY_REQUESTS,
            code,
            message
        );

        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}