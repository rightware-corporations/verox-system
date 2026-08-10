package com.rightware.verox.webhook.domain;

import com.rightware.verox.merchant.domain.Merchant;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookDeliveryTest {

    @Test
    void successfulAttemptBecomesSucceeded() {
        WebhookDelivery delivery = delivery();
        Instant attemptedAt = Instant.parse("2026-08-10T13:10:00Z");

        delivery.recordSuccess(204, attemptedAt);

        assertThat(delivery.getStatus()).isEqualTo(WebhookDeliveryStatus.SUCCEEDED);
        assertThat(delivery.getAttemptCount()).isEqualTo(1);
        assertThat(delivery.getLastStatusCode()).isEqualTo(204);
        assertThat(delivery.getDeliveredAt()).isEqualTo(attemptedAt);
        assertThat(delivery.getLastError()).isNull();
    }

    @Test
    void failedAttemptSchedulesRetryThenExhaustsAtLimit() {
        WebhookDelivery delivery = delivery();
        Instant firstAttempt = Instant.parse("2026-08-10T13:10:00Z");

        delivery.recordFailure(500, "HTTP_STATUS_500", firstAttempt, firstAttempt.plusSeconds(10), 2);

        assertThat(delivery.getStatus()).isEqualTo(WebhookDeliveryStatus.FAILED);
        assertThat(delivery.getAttemptCount()).isEqualTo(1);
        assertThat(delivery.getNextAttemptAt()).isEqualTo(firstAttempt.plusSeconds(10));

        Instant secondAttempt = firstAttempt.plusSeconds(10);
        delivery.recordFailure(503, "HTTP_STATUS_503", secondAttempt, secondAttempt.plusSeconds(20), 2);

        assertThat(delivery.getStatus()).isEqualTo(WebhookDeliveryStatus.EXHAUSTED);
        assertThat(delivery.getAttemptCount()).isEqualTo(2);
        assertThat(delivery.getLastStatusCode()).isEqualTo(503);
        assertThat(delivery.getDeliveredAt()).isNull();
    }

    private WebhookDelivery delivery() {
        Merchant merchant = new Merchant("Webhook Merchant");
        WebhookEndpoint endpoint = new WebhookEndpoint(
            "whep_test",
            merchant,
            "https://merchant.example/webhooks/verox"
        );
        WebhookEvent event = new WebhookEvent(
            "evt_test",
            merchant,
            "payment.confirmed",
            "PAYMENT",
            "pay_test",
            "{\"id\":\"evt_test\"}",
            Instant.parse("2026-08-10T13:00:00Z")
        );
        return new WebhookDelivery("wd_test", event, endpoint, Instant.parse("2026-08-10T13:00:00Z"));
    }
}
