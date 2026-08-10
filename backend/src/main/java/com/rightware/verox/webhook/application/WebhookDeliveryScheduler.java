package com.rightware.verox.webhook.application;

import com.rightware.verox.webhook.domain.WebhookDelivery;
import com.rightware.verox.webhook.domain.WebhookDeliveryStatus;
import com.rightware.verox.webhook.repository.WebhookDeliveryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class WebhookDeliveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(WebhookDeliveryScheduler.class);
    private static final List<WebhookDeliveryStatus> ATTEMPTABLE_STATUSES = List.of(
        WebhookDeliveryStatus.PENDING,
        WebhookDeliveryStatus.FAILED
    );

    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookDeliveryAttemptService attemptService;

    public WebhookDeliveryScheduler(
        WebhookDeliveryRepository deliveryRepository,
        WebhookDeliveryAttemptService attemptService
    ) {
        this.deliveryRepository = deliveryRepository;
        this.attemptService = attemptService;
    }

    @Scheduled(fixedDelayString = "${verox.webhook.delivery.poll-delay-ms:2000}")
    public void deliverDueWebhooks() {
        List<UUID> dueDeliveryIds = deliveryRepository
            .findTop20ByStatusInAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                ATTEMPTABLE_STATUSES,
                Instant.now()
            )
            .stream()
            .map(WebhookDelivery::getId)
            .toList();

        for (UUID deliveryId : dueDeliveryIds) {
            try {
                attemptService.attempt(deliveryId);
            } catch (RuntimeException exception) {
                log.error("Unexpected webhook delivery worker failure for delivery {}", deliveryId, exception);
            }
        }
    }
}
