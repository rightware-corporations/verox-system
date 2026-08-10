package com.rightware.verox.webhook.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class WebhookSignatureService {

    private final byte[] masterSecret;

    public WebhookSignatureService(
        @Value("${verox.webhook.master-secret:verox-dev-webhook-master-secret-change-me}") String masterSecret
    ) {
        if (masterSecret == null || masterSecret.isBlank()) {
            throw new IllegalArgumentException("VEROX webhook master secret is required");
        }
        this.masterSecret = masterSecret.getBytes(StandardCharsets.UTF_8);
    }

    public String signingSecret(String endpointPublicId) {
        byte[] derived = hmac(masterSecret, endpointPublicId.getBytes(StandardCharsets.UTF_8));
        return "whsec_" + Base64.getUrlEncoder().withoutPadding().encodeToString(derived);
    }

    public String signatureHeader(String endpointPublicId, String rawPayload, Instant signedAt) {
        Instant effectiveSignedAt = signedAt == null ? Instant.now() : signedAt;
        long timestamp = effectiveSignedAt.getEpochSecond();
        String signedPayload = timestamp + "." + rawPayload;
        byte[] secret = signingSecret(endpointPublicId).getBytes(StandardCharsets.UTF_8);
        String signature = HexFormat.of().formatHex(hmac(secret, signedPayload.getBytes(StandardCharsets.UTF_8)));
        return "t=" + timestamp + ",v1=" + signature;
    }

    private byte[] hmac(byte[] key, byte[] value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to compute webhook HMAC", exception);
        }
    }
}
