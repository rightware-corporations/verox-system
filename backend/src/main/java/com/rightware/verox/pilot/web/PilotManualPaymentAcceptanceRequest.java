package com.rightware.verox.pilot.web;

import jakarta.validation.constraints.Size;

public record PilotManualPaymentAcceptanceRequest(
    @Size(max = 255) String reason
) {
}
