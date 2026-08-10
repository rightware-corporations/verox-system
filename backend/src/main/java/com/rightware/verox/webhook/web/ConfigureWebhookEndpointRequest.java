package com.rightware.verox.webhook.web;

import jakarta.validation.constraints.NotBlank;

public record ConfigureWebhookEndpointRequest(
    @NotBlank String url
) {
}
