package com.rightware.verox.verification.matching;

public record VerificationMatchResult(
    VerificationDecision decision,
    String reason,
    String transactionReference,
    Long amountMinor,
    String currency
) {
    public boolean isMatch() {
        return decision == VerificationDecision.MATCH;
    }
}
