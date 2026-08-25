package com.rightware.verox.webhook.application;

import com.rightware.verox.pilot.application.PaymentManuallyRejectedEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PaymentManuallyRejectedWebhookListenerTest {
    @Test
    void enqueuesManualRejectionEvent() {
        WebhookOutboxService outbox = mock(WebhookOutboxService.class);
        PaymentManuallyRejectedWebhookListener listener = new PaymentManuallyRejectedWebhookListener(outbox);
        PaymentManuallyRejectedEvent event = event();
        listener.onPaymentManuallyRejected(event);
        verify(outbox).enqueuePaymentManuallyRejected(event);
    }

    @Test
    void enqueueFailureDoesNotEscapeListener() {
        WebhookOutboxService outbox = mock(WebhookOutboxService.class);
        PaymentManuallyRejectedWebhookListener listener = new PaymentManuallyRejectedWebhookListener(outbox);
        PaymentManuallyRejectedEvent event = event();
        doThrow(new RuntimeException("outbox unavailable")).when(outbox).enqueuePaymentManuallyRejected(event);
        assertThatCode(() -> listener.onPaymentManuallyRejected(event)).doesNotThrowAnyException();
    }

    private PaymentManuallyRejectedEvent event() {
        return new PaymentManuallyRejectedEvent(UUID.randomUUID(), "pay_rejected", "cs_rejected", "ORDER-REJECTED", "1.00", "MZN", "PENDING", Instant.parse("2026-08-21T20:30:00Z"));
    }
}
