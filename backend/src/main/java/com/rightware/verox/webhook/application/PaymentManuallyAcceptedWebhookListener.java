package com.rightware.verox.webhook.application;

import com.rightware.verox.pilot.application.PaymentManuallyAcceptedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PaymentManuallyAcceptedWebhookListener {

    private static final Logger log =
        LoggerFactory.getLogger(PaymentManuallyAcceptedWebhookListener.class);

    private final WebhookOutboxService webhookOutboxService;

    public PaymentManuallyAcceptedWebhookListener(
        WebhookOutboxService webhookOutboxService
    ) {
        this.webhookOutboxService = webhookOutboxService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentManuallyAccepted(PaymentManuallyAcceptedEvent event) {
        try {
            webhookOutboxService.enqueuePaymentManuallyAccepted(event);
        } catch (RuntimeException exception) {
            log.error(
                "VEROX Webhook Delivery failed to enqueue payment.manually_accepted for payment {}.",
                event.paymentId(),
                exception
            );
        }
    }
}
