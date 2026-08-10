package com.rightware.verox.webhook.delivery;

import java.util.Map;

public interface WebhookHttpTransport {
    int postJson(String url, String rawPayload, Map<String, String> headers);
}
