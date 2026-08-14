package com.rightware.verox.webhook.application;

import com.rightware.verox.webhook.delivery.WebhookHttpTransport;
import com.rightware.verox.webhook.domain.WebhookDelivery;
import com.rightware.verox.webhook.repository.WebhookDeliveryRepository;
import com.rightware.verox.webhook.security.WebhookDestinationPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class WebhookDeliveryAttemptService {

    private static final Logger log =
        LoggerFactory.getLogger(WebhookDeliveryAttemptService.class);

    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookSignatureService signatureService;
    private final WebhookHttpTransport httpTransport;
    private final WebhookDestinationPolicy destinationPolicy;
    private final int maxAttempts;
    private final long baseRetrySeconds;
    private final long maxRetrySeconds;

    public WebhookDeliveryAttemptService(
        WebhookDeliveryRepository deliveryRepository,
        WebhookSignatureService signatureService,
        WebhookHttpTransport httpTransport,
        WebhookDestinationPolicy destinationPolicy,
        @Value("${verox.webhook.delivery.max-attempts:8}") int maxAttempts,
        @Value("${verox.webhook.delivery.base-retry-seconds:10}") long baseRetrySeconds,
        @Value("${verox.webhook.delivery.max-retry-seconds:900}") long maxRetrySeconds
    ) {
        this.deliveryRepository = deliveryRepository;
        this.signatureService = signatureService;
        this.httpTransport = httpTransport;
        this.destinationPolicy = destinationPolicy;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.baseRetrySeconds = Math.max(1, baseRetrySeconds);
        this.maxRetrySeconds = Math.max(this.baseRetrySeconds, maxRetrySeconds);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void attempt(UUID deliveryId) {
        WebhookDelivery delivery =
            deliveryRepository.findById(deliveryId).orElse(null);

        if (delivery == null) {
            return;
        }

        Instant attemptedAt = Instant.now();

        if (!delivery.isAttemptableAt(attemptedAt)) {
            return;
        }

        String validatedUrl;
        try {
            // Security-critical revalidation on every delivery attempt.
            // DNS is resolved and checked again here, not trusted from
            // endpoint-configuration time.
            validatedUrl =
                destinationPolicy.validate(delivery.getEndpoint().getUrl());
        } catch (RuntimeException exception) {
            recordFailure(
                delivery,
                null,
                "WEBHOOK_DESTINATION_REJECTED",
                attemptedAt
            );
            return;
        }

        String payload = delivery.getEvent().getPayloadJson();
        String endpointPublicId =
            delivery.getEndpoint().getPublicId();

        String signature =
            signatureService.signatureHeader(
                endpointPublicId,
                payload,
                attemptedAt
            );

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(
            "VEROX-Event-Id",
            delivery.getEvent().getPublicId()
        );
        headers.put(
            "VEROX-Event-Type",
            delivery.getEvent().getType()
        );
        headers.put(
            "VEROX-Delivery-Id",
            delivery.getPublicId()
        );
        headers.put(
            "VEROX-Signature",
            signature
        );

        int statusCode;

        try {
            statusCode =
                httpTransport.postJson(
                    validatedUrl,
                    payload,
                    headers
                );
        } catch (RuntimeException exception) {
            recordFailure(
                delivery,
                null,
                safeError(exception),
                attemptedAt
            );
            return;
        }

        if (statusCode >= 200 && statusCode < 300) {
            delivery.recordSuccess(statusCode, attemptedAt);
            deliveryRepository.save(delivery);

            log.info(
                "Webhook delivery {} succeeded with HTTP {}",
                delivery.getPublicId(),
                statusCode
            );
            return;
        }

        recordFailure(
            delivery,
            statusCode,
            "HTTP_STATUS_" + statusCode,
            attemptedAt
        );
    }

    private void recordFailure(
        WebhookDelivery delivery,
        Integer statusCode,
        String error,
        Instant attemptedAt
    ) {
        Instant retryAt =
            attemptedAt.plusSeconds(
                retryDelaySeconds(delivery.getAttemptCount())
            );

        delivery.recordFailure(
            statusCode,
            error,
            attemptedAt,
            retryAt,
            maxAttempts
        );

        deliveryRepository.save(delivery);

        log.warn(
            "Webhook delivery {} failed on attempt {} and is now {}",
            delivery.getPublicId(),
            delivery.getAttemptCount(),
            delivery.getStatus()
        );
    }

    private long retryDelaySeconds(int previousAttempts) {
        int exponent =
            Math.min(Math.max(previousAttempts, 0), 20);

        long multiplier = 1L << exponent;

        if (baseRetrySeconds > maxRetrySeconds / multiplier) {
            return maxRetrySeconds;
        }

        return Math.min(
            maxRetrySeconds,
            baseRetrySeconds * multiplier
        );
    }

    private String safeError(RuntimeException exception) {
        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }

        return exception.getClass().getSimpleName()
            + ": "
            + message;
    }
}