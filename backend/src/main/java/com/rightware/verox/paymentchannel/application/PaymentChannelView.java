package com.rightware.verox.paymentchannel.application;

import java.time.Instant;

public record PaymentChannelView(
    String provider,
    String displayName,
    String kind,
    String status,
    String recipientDisplay,
    String recipientName,
    String instructions,
    Instant updatedAt
) {
}
