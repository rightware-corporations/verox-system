package com.rightware.verox.common.web;

import com.rightware.verox.common.ratelimit.RateLimitExceededException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerRateLimitTest {

    @Test
    void returnsTooManyRequestsWithRetryAfterHeader() {
        ApiExceptionHandler handler = new ApiExceptionHandler();

        RateLimitExceededException exception =
            new RateLimitExceededException(
                "RATE_LIMIT_EXCEEDED",
                "Too many requests.",
                17
            );

        ResponseEntity<ApiExceptionHandler.ErrorEnvelope> response =
            handler.handleRateLimitExceeded(exception);

        assertThat(response.getStatusCode())
            .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        assertThat(
            response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)
        ).isEqualTo("17");

        assertThat(response.getBody()).isNotNull();

        assertThat(response.getBody().error().code())
            .isEqualTo("RATE_LIMIT_EXCEEDED");

        assertThat(response.getBody().error().message())
            .isEqualTo("Too many requests.");
    }

    @Test
    void normalizesRetryAfterToAtLeastOneSecond() {
        RateLimitExceededException exception =
            new RateLimitExceededException(
                "RATE_LIMIT_EXCEEDED",
                "Too many requests.",
                0
            );

        assertThat(exception.getRetryAfterSeconds())
            .isEqualTo(1);
    }
}