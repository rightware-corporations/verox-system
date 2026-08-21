package com.rightware.verox.pilot.application;

import java.time.Instant;
import java.util.UUID;

public record PaymentManuallyAcceptedEvent(
    UUID merchantId,
    String paymentId,
    String checkoutSessionId,
    String externalReference,
    String amount,
    String currency,
    String status,
    Instant manuallyAcceptedAt
) {
}
