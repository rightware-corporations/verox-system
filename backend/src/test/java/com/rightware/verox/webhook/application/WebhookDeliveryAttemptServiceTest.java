package com.rightware.verox.webhook.application;

import com.rightware.verox.merchant.domain.Merchant;
import com.rightware.verox.webhook.delivery.WebhookHttpTransport;
import com.rightware.verox.webhook.domain.WebhookDelivery;
import com.rightware.verox.webhook.domain.WebhookDeliveryStatus;
import com.rightware.verox.webhook.domain.WebhookEndpoint;
import com.rightware.verox.webhook.domain.WebhookEvent;
import com.rightware.verox.webhook.repository.WebhookDeliveryRepository;
import com.rightware.verox.webhook.security.WebhookDestinationPolicy;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebhookDeliveryAttemptServiceTest {

    @Test
    void postsRawPayloadWithVeroxSignatureHeadersAndMarksSuccess() {
        Fixture fixture = fixture();

        when(fixture.repository().findById(fixture.delivery().getId()))
            .thenReturn(Optional.of(fixture.delivery()));

        when(fixture.destinationPolicy().validate(anyString()))
            .thenReturn("https://merchant.example/webhooks/verox");

        when(fixture.transport().postJson(anyString(), anyString(), anyMap()))
            .thenReturn(204);

        fixture.service().attempt(fixture.delivery().getId());

        assertThat(fixture.delivery().getStatus())
            .isEqualTo(WebhookDeliveryStatus.SUCCEEDED);
        assertThat(fixture.delivery().getAttemptCount()).isEqualTo(1);
        assertThat(fixture.delivery().getLastStatusCode()).isEqualTo(204);
        assertThat(fixture.delivery().getDeliveredAt()).isNotNull();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> headersCaptor =
            ArgumentCaptor.forClass(Map.class);

        verify(fixture.destinationPolicy())
            .validate("https://merchant.example/webhooks/verox");

        verify(fixture.transport()).postJson(
            org.mockito.ArgumentMatchers.eq(
                "https://merchant.example/webhooks/verox"
            ),
            org.mockito.ArgumentMatchers.eq(
                "{\"id\":\"evt_test\"}"
            ),
            headersCaptor.capture()
        );

        assertThat(headersCaptor.getValue().get("VEROX-Event-Id"))
            .isEqualTo("evt_test");
        assertThat(headersCaptor.getValue().get("VEROX-Event-Type"))
            .isEqualTo("payment.confirmed");
        assertThat(headersCaptor.getValue().get("VEROX-Delivery-Id"))
            .isEqualTo("wd_test");
        assertThat(headersCaptor.getValue().get("VEROX-Signature"))
            .startsWith("t=")
            .contains(",v1=");

        verify(fixture.repository()).save(fixture.delivery());
    }

    @Test
    void non2xxResponseBecomesFailedAndSchedulesRetry() {
        Fixture fixture = fixture();

        when(fixture.repository().findById(fixture.delivery().getId()))
            .thenReturn(Optional.of(fixture.delivery()));

        when(fixture.destinationPolicy().validate(anyString()))
            .thenReturn("https://merchant.example/webhooks/verox");

        when(fixture.transport().postJson(anyString(), anyString(), anyMap()))
            .thenReturn(503);

        fixture.service().attempt(fixture.delivery().getId());

        assertThat(fixture.delivery().getStatus())
            .isEqualTo(WebhookDeliveryStatus.FAILED);
        assertThat(fixture.delivery().getAttemptCount()).isEqualTo(1);
        assertThat(fixture.delivery().getLastStatusCode()).isEqualTo(503);
        assertThat(fixture.delivery().getLastError())
            .isEqualTo("HTTP_STATUS_503");
        assertThat(fixture.delivery().getNextAttemptAt())
            .isAfter(fixture.delivery().getLastAttemptAt());

        verify(fixture.repository()).save(fixture.delivery());
    }

    @Test
    void rejectedDestinationFailsBeforeHttpTransport() {
        Fixture fixture = fixture();

        when(fixture.repository().findById(fixture.delivery().getId()))
            .thenReturn(Optional.of(fixture.delivery()));

        when(fixture.destinationPolicy().validate(anyString()))
            .thenThrow(
                new IllegalArgumentException(
                    "Webhook destination is not public"
                )
            );

        fixture.service().attempt(fixture.delivery().getId());

        assertThat(fixture.delivery().getStatus())
            .isEqualTo(WebhookDeliveryStatus.FAILED);
        assertThat(fixture.delivery().getAttemptCount()).isEqualTo(1);
        assertThat(fixture.delivery().getLastStatusCode()).isNull();
        assertThat(fixture.delivery().getLastError())
            .isEqualTo("WEBHOOK_DESTINATION_REJECTED");
        assertThat(fixture.delivery().getNextAttemptAt())
            .isAfter(fixture.delivery().getLastAttemptAt());

        verify(fixture.destinationPolicy())
            .validate("https://merchant.example/webhooks/verox");

        verify(fixture.transport(), never()).postJson(
            anyString(),
            anyString(),
            anyMap()
        );

        verify(fixture.repository()).save(fixture.delivery());
    }

    private Fixture fixture() {
        WebhookDeliveryRepository repository =
            mock(WebhookDeliveryRepository.class);

        WebhookHttpTransport transport =
            mock(WebhookHttpTransport.class);

        WebhookDestinationPolicy destinationPolicy =
            mock(WebhookDestinationPolicy.class);

        WebhookSignatureService signatureService =
            new WebhookSignatureService(
                "test-webhook-master-secret"
            );

        Merchant merchant =
            new Merchant("Webhook Merchant");

        WebhookEndpoint endpoint =
            new WebhookEndpoint(
                "whep_test",
                merchant,
                "https://merchant.example/webhooks/verox"
            );

        WebhookEvent event =
            new WebhookEvent(
                "evt_test",
                merchant,
                "payment.confirmed",
                "PAYMENT",
                "pay_test",
                "{\"id\":\"evt_test\"}",
                Instant.now().minusSeconds(30)
            );

        WebhookDelivery delivery =
            new WebhookDelivery(
                "wd_test",
                event,
                endpoint,
                Instant.now().minusSeconds(1)
            );

        WebhookDeliveryAttemptService service =
            new WebhookDeliveryAttemptService(
                repository,
                signatureService,
                transport,
                destinationPolicy,
                8,
                10,
                900
            );

        return new Fixture(
            repository,
            transport,
            destinationPolicy,
            delivery,
            service
        );
    }

    private record Fixture(
        WebhookDeliveryRepository repository,
        WebhookHttpTransport transport,
        WebhookDestinationPolicy destinationPolicy,
        WebhookDelivery delivery,
        WebhookDeliveryAttemptService service
    ) {
    }
}