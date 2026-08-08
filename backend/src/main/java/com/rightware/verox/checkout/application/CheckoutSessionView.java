package com.rightware.verox.checkout.application;

import java.time.Instant;

public record CheckoutSessionView(
    String id,
    String paymentId,
    String externalReference,
    String status,
    String paymentStatus,
    String amount,
    String currency,
    String description,
    String checkoutUrl,
    Instant expiresAt
) {
}
