package com.rightware.verox.webhook.application;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookSignatureServiceTest {

    @Test
    void derivesStableSecretAndSignsTimestampPlusRawPayload() {
        WebhookSignatureService service = new WebhookSignatureService("test-master-secret");

        String secret = service.signingSecret("whep_test123");
        String signature = service.signatureHeader(
            "whep_test123",
            "{\"id\":\"evt_test\"}",
            Instant.ofEpochSecond(1_800_000_000L)
        );

        assertThat(secret).startsWith("whsec_");
        assertThat(service.signingSecret("whep_test123")).isEqualTo(secret);
        assertThat(signature).startsWith("t=1800000000,v1=");
        assertThat(signature).hasSize(80);
    }
}
