package com.rightware.verox.webhook.application;

public record WebhookEndpointView(
    String id,
    String url,
    String status,
    String signingSecret
) {
}
