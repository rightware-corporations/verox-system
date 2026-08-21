package com.rightware.verox.webhook.application;

import com.rightware.verox.pilot.application.PaymentManuallyAcceptedEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PaymentManuallyAcceptedWebhookListenerTest {

    @Test
    void enqueuesManualAcceptanceEvent() {
        WebhookOutboxService outbox = mock(WebhookOutboxService.class);
        PaymentManuallyAcceptedWebhookListener listener =
            new PaymentManuallyAcceptedWebhookListener(outbox);

        PaymentManuallyAcceptedEvent event = event();

        listener.onPaymentManuallyAccepted(event);

        verify(outbox).enqueuePaymentManuallyAccepted(event);
    }

    @Test
    void enqueueFailureDoesNotEscapeListener() {
        WebhookOutboxService outbox = mock(WebhookOutboxService.class);
        PaymentManuallyAcceptedWebhookListener listener =
            new PaymentManuallyAcceptedWebhookListener(outbox);

        PaymentManuallyAcceptedEvent event = event();

        doThrow(new RuntimeException("outbox unavailable"))
            .when(outbox).enqueuePaymentManuallyAccepted(event);

        assertThatCode(() -> listener.onPaymentManuallyAccepted(event))
            .doesNotThrowAnyException();
    }

    private PaymentManuallyAcceptedEvent event() {
        return new PaymentManuallyAcceptedEvent(
            UUID.randomUUID(),
            "pay_manual",
            "cs_manual",
            "ORDER-MANUAL",
            "1.00",
            "MZN",
            "PENDING",
            Instant.parse("2026-08-21T20:30:00Z")
        );
    }
}
