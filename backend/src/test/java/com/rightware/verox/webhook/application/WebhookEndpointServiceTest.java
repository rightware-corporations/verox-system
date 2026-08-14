package com.rightware.verox.webhook.application;

import com.rightware.verox.common.id.ResourceIdGenerator;
import com.rightware.verox.common.web.ApiException;
import com.rightware.verox.merchant.domain.Merchant;
import com.rightware.verox.merchant.repository.MerchantRepository;
import com.rightware.verox.webhook.domain.WebhookEndpoint;
import com.rightware.verox.webhook.repository.WebhookEndpointRepository;
import com.rightware.verox.webhook.security.WebhookDestinationPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WebhookEndpointServiceTest {

    @Test
    void rejectedDestinationBecomesInvalidWebhookUrlBeforePersistence() {
        MerchantRepository merchantRepository =
            mock(MerchantRepository.class);

        WebhookEndpointRepository endpointRepository =
            mock(WebhookEndpointRepository.class);

        ResourceIdGenerator idGenerator =
            mock(ResourceIdGenerator.class);

        WebhookDestinationPolicy destinationPolicy =
            mock(WebhookDestinationPolicy.class);

        WebhookSignatureService signatureService =
            new WebhookSignatureService(
                "test-webhook-master-secret"
            );

        WebhookEndpointService service =
            new WebhookEndpointService(
                merchantRepository,
                endpointRepository,
                idGenerator,
                signatureService,
                destinationPolicy
            );

        when(destinationPolicy.validate("https://127.0.0.1/webhook"))
            .thenThrow(
                new IllegalArgumentException(
                    "Webhook destination is not public"
                )
            );

        assertThatThrownBy(() ->
            service.configure(
                java.util.UUID.randomUUID(),
                "https://127.0.0.1/webhook"
            )
        )
            .isInstanceOfSatisfying(
                ApiException.class,
                exception -> {
                    assertThat(exception.getStatus())
                        .isEqualTo(HttpStatus.BAD_REQUEST);

                    assertThat(exception.getCode())
                        .isEqualTo("INVALID_WEBHOOK_URL");

                    assertThat(exception.getMessage())
                        .contains("not public");
                }
            );

        verify(destinationPolicy)
            .validate("https://127.0.0.1/webhook");

        verifyNoInteractions(
            merchantRepository,
            endpointRepository,
            idGenerator
        );
    }

    @Test
    void validatedDestinationCanBeConfigured() {
        MerchantRepository merchantRepository =
            mock(MerchantRepository.class);

        WebhookEndpointRepository endpointRepository =
            mock(WebhookEndpointRepository.class);

        ResourceIdGenerator idGenerator =
            mock(ResourceIdGenerator.class);

        WebhookDestinationPolicy destinationPolicy =
            mock(WebhookDestinationPolicy.class);

        WebhookSignatureService signatureService =
            new WebhookSignatureService(
                "test-webhook-master-secret"
            );

        Merchant merchant =
            new Merchant("SEC-035 Merchant");

        String url =
            "https://merchant.example/webhooks/verox";

        when(destinationPolicy.validate(url))
            .thenReturn(url);

        when(merchantRepository.findById(merchant.getId()))
            .thenReturn(Optional.of(merchant));

        when(endpointRepository.findByMerchantId(merchant.getId()))
            .thenReturn(Optional.empty());

        when(idGenerator.generate("whep"))
            .thenReturn("whep_sec035");

        WebhookEndpointService service =
            new WebhookEndpointService(
                merchantRepository,
                endpointRepository,
                idGenerator,
                signatureService,
                destinationPolicy
            );

        WebhookEndpointView view =
            service.configure(
                merchant.getId(),
                url
            );

        assertThat(view.id())
            .isEqualTo("whep_sec035");

        assertThat(view.url())
            .isEqualTo(url);

        assertThat(view.status())
            .isEqualTo("ACTIVE");

        assertThat(view.signingSecret())
            .startsWith("whsec_");

        verify(destinationPolicy)
            .validate(url);

        verify(endpointRepository)
            .save(any(WebhookEndpoint.class));
    }
}