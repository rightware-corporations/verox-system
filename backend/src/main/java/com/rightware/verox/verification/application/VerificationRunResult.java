package com.rightware.verox.verification.application;

public record VerificationRunResult(
    String paymentId,
    VerificationRunStatus status,
    String reason,
    String providerEvidenceId,
    String transactionReference
) {
}
