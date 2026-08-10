package com.rightware.verox.verification.mpesa;

import com.rightware.verox.evidence.domain.EvidenceOrigin;

public record ParsedMpesaMessage(
    EvidenceOrigin origin,
    String transactionReference,
    Long amountMinor,
    String currency,
    boolean recognizedFormat
) {
    public boolean isMatchReady() {
        return recognizedFormat
            && transactionReference != null
            && amountMinor != null
            && currency != null;
    }
}
