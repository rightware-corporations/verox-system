package com.rightware.verox.bridge.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record BridgeEvidenceRequest(
    @NotBlank
    @Size(max = 4096)
    String content,

    @JsonProperty("received_at")
    Instant receivedAt
) {
}
