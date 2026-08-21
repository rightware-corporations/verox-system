package com.rightware.verox.pilot.application;

import java.time.Instant;

public record PilotManualPaymentAcceptanceView(
    String paymentId,
    String status,
    String reason,
    Instant acceptedAt
) {
}
