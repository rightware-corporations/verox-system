package com.rightware.verox.checkout.application;

import java.time.Instant;
import java.util.List;

public record HostedCheckoutBootstrapView(
    String checkoutSessionId,
    String paymentId,
    String merchantDisplayName,
    String externalReference,
    String description,
    String amount,
    String currency,
    String checkoutStatus,
    String paymentStatus,
    String effectivePaymentStatus,
    Instant expiresAt,
    String successUrl,
    String cancelUrl,
    List<PaymentChannelView> paymentChannels
) {
    public record PaymentChannelView(
        String provider,
        String displayName,
        String kind,
        boolean enabled,
        String recipientDisplay,
        String recipientName,
        String instructions
    ) {
    }
}
