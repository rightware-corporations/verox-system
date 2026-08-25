package com.rightware.verox.webhook.application;

import com.rightware.verox.pilot.application.PaymentManuallyRejectedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PaymentManuallyRejectedWebhookListener {
    private static final Logger log = LoggerFactory.getLogger(PaymentManuallyRejectedWebhookListener.class);
    private final WebhookOutboxService webhookOutboxService;
    public PaymentManuallyRejectedWebhookListener(WebhookOutboxService webhookOutboxService) { this.webhookOutboxService = webhookOutboxService; }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentManuallyRejected(PaymentManuallyRejectedEvent event) {
        try { webhookOutboxService.enqueuePaymentManuallyRejected(event); }
        catch (RuntimeException exception) { log.error("VEROX Webhook Delivery failed to enqueue payment.manually_rejected for payment {}.", event.paymentId(), exception); }
    }
}
