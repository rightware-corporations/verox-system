package com.rightware.verox.common.web;

import com.rightware.verox.common.ratelimit.RateLimitExceededException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    ResponseEntity<ErrorEnvelope> handleRateLimitExceeded(
        RateLimitExceededException exception
    ) {
        return ResponseEntity
            .status(exception.getStatus())
            .header(
                HttpHeaders.RETRY_AFTER,
                Long.toString(exception.getRetryAfterSeconds())
            )
            .body(
                error(
                    exception.getCode(),
                    exception.getMessage()
                )
            );
    }
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorEnvelope> handleApiException(ApiException exception) {
        return ResponseEntity
            .status(exception.getStatus())
            .body(error(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorEnvelope> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
            .orElse("Request validation failed.");

        return ResponseEntity
            .badRequest()
            .body(error("INVALID_REQUEST", message));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ResponseEntity<ErrorEnvelope> handleMissingHeader(MissingRequestHeaderException exception) {
        return ResponseEntity
            .badRequest()
            .body(error("MISSING_HEADER", "Required header is missing: " + exception.getHeaderName()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorEnvelope> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error("INVALID_JSON", "Request body is invalid or cannot be read."));
    }

    private ErrorEnvelope error(String code, String message) {
        return new ErrorEnvelope(new ApiError(code, message));
    }

    public record ErrorEnvelope(ApiError error) {
    }

    public record ApiError(String code, String message) {
    }
}
