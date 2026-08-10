package com.rightware.verox.payment.application;

import java.time.Instant;
import java.util.UUID;

public record PaymentConfirmedEvent(
    UUID merchantId,
    String paymentId,
    String checkoutSessionId,
    String externalReference,
    String amount,
    String currency,
    String provider,
    String providerTransactionReference,
    Instant confirmedAt
) {
}
