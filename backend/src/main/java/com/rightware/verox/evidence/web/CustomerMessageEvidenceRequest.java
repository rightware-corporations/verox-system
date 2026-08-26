package com.rightware.verox.evidence.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerMessageEvidenceRequest(
    @NotBlank
    @Size(max = 64)
    String provider,
    @NotBlank
    @Size(max = 4096)
    String content
) {
}
