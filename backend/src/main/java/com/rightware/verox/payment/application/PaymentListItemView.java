package com.rightware.verox.payment.application;

import java.time.Instant;

public record PaymentListItemView(
    String id,
    String checkoutSessionId,
    String externalReference,
    String description,
    String status,
    String effectiveStatus,
    boolean attentionRequired,
    String amount,
    String currency,
    String provider,
    Instant createdAt,
    Instant confirmedAt,
    Instant manuallyAcceptedAt
) {
}
