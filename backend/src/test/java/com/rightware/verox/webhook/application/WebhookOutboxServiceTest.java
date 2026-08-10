package com.rightware.verox.webhook.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rightware.verox.common.id.ResourceIdGenerator;
import com.rightware.verox.merchant.domain.Merchant;
import com.rightware.verox.merchant.repository.MerchantRepository;
import com.rightware.verox.payment.application.PaymentConfirmedEvent;
import com.rightware.verox.webhook.domain.WebhookDelivery;
import com.rightware.verox.webhook.domain.WebhookEndpoint;
import com.rightware.verox.webhook.domain.WebhookEndpointStatus;
import com.rightware.verox.webhook.domain.WebhookEvent;
import com.rightware.verox.webhook.repository.WebhookDeliveryRepository;
import com.rightware.verox.webhook.repository.WebhookEndpointRepository;
import com.rightware.verox.webhook.repository.WebhookEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebhookOutboxServiceTest {

    @Test
    void persistsPaymentConfirmedEventAndPendingDelivery() {
        MerchantRepository merchantRepository = mock(MerchantRepository.class);
        WebhookEndpointRepository endpointRepository = mock(WebhookEndpointRepository.class);
        WebhookEventRepository eventRepository = mock(WebhookEventRepository.class);
        WebhookDeliveryRepository deliveryRepository = mock(WebhookDeliveryRepository.class);
        ResourceIdGenerator idGenerator = mock(ResourceIdGenerator.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

        Merchant merchant = new Merchant("Event Merchant");
        WebhookEndpoint endpoint = new WebhookEndpoint("whep_test", merchant, "https://merchant.example/webhooks/verox");
        PaymentConfirmedEvent confirmed = new PaymentConfirmedEvent(
            merchant.getId(), "pay_test", "cs_test", "ORDER-1", "1.00", "MZN", "MPESA", "DH10L1OJRUS",
            Instant.parse("2026-08-10T12:00:00Z")
        );

        when(eventRepository.findByMerchantIdAndTypeAndAggregateTypeAndAggregatePublicId(
            merchant.getId(), "payment.confirmed", "PAYMENT", "pay_test"
        )).thenReturn(Optional.empty());
        when(merchantRepository.findById(merchant.getId())).thenReturn(Optional.of(merchant));
        when(endpointRepository.findByMerchantIdAndStatus(merchant.getId(), WebhookEndpointStatus.ACTIVE))
            .thenReturn(Optional.of(endpoint));
        when(idGenerator.generate("evt")).thenReturn("evt_test");
        when(idGenerator.generate("wd")).thenReturn("wd_test");
        when(eventRepository.save(any(WebhookEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryRepository.findByEventIdAndEndpointId(any(), any())).thenReturn(Optional.empty());
        when(deliveryRepository.save(any(WebhookDelivery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WebhookOutboxService service = new WebhookOutboxService(
            merchantRepository, endpointRepository, eventRepository, deliveryRepository, idGenerator, objectMapper
        );

        WebhookEvent event = service.enqueuePaymentConfirmed(confirmed);

        assertThat(event.getPublicId()).isEqualTo("evt_test");
        assertThat(event.getType()).isEqualTo("payment.confirmed");
        assertThat(event.getPayloadJson()).contains("\"payment_id\":\"pay_test\"");
        assertThat(event.getPayloadJson()).contains("\"status\":\"CONFIRMED\"");

        ArgumentCaptor<WebhookDelivery> deliveryCaptor = ArgumentCaptor.forClass(WebhookDelivery.class);
        verify(deliveryRepository).save(deliveryCaptor.capture());
        assertThat(deliveryCaptor.getValue().getPublicId()).isEqualTo("wd_test");
        assertThat(deliveryCaptor.getValue().getStatus().name()).isEqualTo("PENDING");
    }
}
