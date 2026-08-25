package com.rightware.verox.pilot.application;

import java.time.Instant;

public record PilotManualPaymentRejectionView(
    String paymentId,
    String status,
    String reason,
    Instant rejectedAt
) {}
