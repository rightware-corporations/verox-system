package com.rightware.verox.payment.application;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

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
    Instant manuallyAcceptedAt,
    Instant manuallyRejectedAt,
    String manualDecisionReason,
    @JsonInclude(JsonInclude.Include.NON_NULL) CustomerEvidenceView customerEvidence
) {
    public record CustomerEvidenceView(
        String channel,
        String amount,
        String externalReference,
        Instant submittedAt,
        String message
    ) {}
}
