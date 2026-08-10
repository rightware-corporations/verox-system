package com.rightware.verox.webhook.application;

import com.rightware.verox.payment.application.PaymentConfirmedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PaymentConfirmedWebhookListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentConfirmedWebhookListener.class);

    private final WebhookOutboxService webhookOutboxService;

    public PaymentConfirmedWebhookListener(WebhookOutboxService webhookOutboxService) {
        this.webhookOutboxService = webhookOutboxService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentConfirmed(PaymentConfirmedEvent event) {
        try {
            webhookOutboxService.enqueuePaymentConfirmed(event);
        } catch (RuntimeException exception) {
            log.error(
                "VEROX Webhook Delivery failed to enqueue payment.confirmed for payment {}.",
                event.paymentId(),
                exception
            );
        }
    }
}
