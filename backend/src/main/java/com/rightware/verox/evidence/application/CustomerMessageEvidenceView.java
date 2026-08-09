package com.rightware.verox.evidence.application;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record CustomerMessageEvidenceView(
    String id,
    @JsonProperty("checkout_session_id") String checkoutSessionId,
    @JsonProperty("payment_id") String paymentId,
    String origin,
    String kind,
    @JsonProperty("ingest_source") String ingestSource,
    String provider,
    @JsonProperty("received_at") Instant receivedAt
) {
}
