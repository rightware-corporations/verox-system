package com.rightware.verox.bridge.application;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record BridgeEvidenceView(
    String id,
    @JsonProperty("bridge_id") String bridgeId,
    String origin,
    String kind,
    String provider,
    @JsonProperty("received_at") Instant receivedAt
) {
}
