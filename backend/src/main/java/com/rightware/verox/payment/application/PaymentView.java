package com.rightware.verox.payment.application;

import java.time.Instant;

public record PaymentView(
    String id,
    String checkoutSessionId,
    String externalReference,
    String status,
    String effectiveStatus,
    String amount,
    String currency,
    String provider,
    Instant confirmedAt,
    Instant manuallyAcceptedAt
) {
}
