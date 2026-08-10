package com.rightware.verox.evidence.application;

import com.rightware.verox.evidence.domain.EvidenceOrigin;

import java.util.UUID;

public record EvidenceIngestedEvent(
    UUID merchantId,
    UUID paymentId,
    EvidenceOrigin origin,
    String evidencePublicId
) {
}
