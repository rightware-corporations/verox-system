package com.rightware.verox.webhook.delivery;

public class WebhookTransportException extends RuntimeException {
    public WebhookTransportException(String message, Throwable cause) {
        super(message, cause);
    }
}
