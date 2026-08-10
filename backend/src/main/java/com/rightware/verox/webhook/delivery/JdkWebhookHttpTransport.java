package com.rightware.verox.webhook.delivery;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Component
public class JdkWebhookHttpTransport implements WebhookHttpTransport {

    private final HttpClient httpClient;
    private final Duration requestTimeout;

    public JdkWebhookHttpTransport(
        @Value("${verox.webhook.delivery.connect-timeout-seconds:5}") long connectTimeoutSeconds,
        @Value("${verox.webhook.delivery.request-timeout-seconds:10}") long requestTimeoutSeconds
    ) {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(Math.max(1, connectTimeoutSeconds)))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
        this.requestTimeout = Duration.ofSeconds(Math.max(1, requestTimeoutSeconds));
    }

    @Override
    public int postJson(String url, String rawPayload, Map<String, String> headers) {
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url))
                .timeout(requestTimeout)
                .header("Content-Type", "application/json")
                .header("User-Agent", "VEROX-Webhooks/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(rawPayload));

            headers.forEach(request::header);

            HttpResponse<Void> response = httpClient.send(
                request.build(),
                HttpResponse.BodyHandlers.discarding()
            );
            return response.statusCode();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new WebhookTransportException("Webhook HTTP request was interrupted", exception);
        } catch (Exception exception) {
            throw new WebhookTransportException("Webhook HTTP request failed: " + exception.getClass().getSimpleName(), exception);
        }
    }
}
