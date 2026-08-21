package com.rightware.verox.webhook.application;

import com.rightware.verox.common.id.ResourceIdGenerator;
import com.rightware.verox.merchant.domain.Merchant;
import com.rightware.verox.merchant.repository.MerchantRepository;
import com.rightware.verox.payment.application.PaymentConfirmedEvent;
import com.rightware.verox.pilot.application.PaymentManuallyAcceptedEvent;
import com.rightware.verox.webhook.domain.WebhookDelivery;
import com.rightware.verox.webhook.domain.WebhookEndpoint;
import com.rightware.verox.webhook.domain.WebhookEndpointStatus;
import com.rightware.verox.webhook.domain.WebhookEvent;
import com.rightware.verox.webhook.repository.WebhookDeliveryRepository;
import com.rightware.verox.webhook.repository.WebhookEndpointRepository;
import com.rightware.verox.webhook.repository.WebhookEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

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
        JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();

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
            merchantRepository, endpointRepository, eventRepository, deliveryRepository, idGenerator, jsonMapper
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
    @Test
    void persistsPaymentManuallyAcceptedEventAndPendingDelivery() {
        MerchantRepository merchantRepository = mock(MerchantRepository.class);
        WebhookEndpointRepository endpointRepository = mock(WebhookEndpointRepository.class);
        WebhookEventRepository eventRepository = mock(WebhookEventRepository.class);
        WebhookDeliveryRepository deliveryRepository = mock(WebhookDeliveryRepository.class);
        ResourceIdGenerator idGenerator = mock(ResourceIdGenerator.class);
        JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();

        Merchant merchant = new Merchant("Pilot Merchant");
        WebhookEndpoint endpoint = new WebhookEndpoint(
            "whep_manual",
            merchant,
            "https://merchant.example/webhooks/verox"
        );

        PaymentManuallyAcceptedEvent accepted = new PaymentManuallyAcceptedEvent(
            merchant.getId(),
            "pay_manual",
            "cs_manual",
            "ORDER-MANUAL",
            "1.00",
            "MZN",
            "PENDING",
            Instant.parse("2026-08-21T20:30:00Z")
        );

        when(eventRepository.findByMerchantIdAndTypeAndAggregateTypeAndAggregatePublicId(
            merchant.getId(),
            "payment.manually_accepted",
            "PAYMENT",
            "pay_manual"
        )).thenReturn(Optional.empty());

        when(merchantRepository.findById(merchant.getId()))
            .thenReturn(Optional.of(merchant));

        when(endpointRepository.findByMerchantIdAndStatus(
            merchant.getId(),
            WebhookEndpointStatus.ACTIVE
        )).thenReturn(Optional.of(endpoint));

        when(idGenerator.generate("evt")).thenReturn("evt_manual");
        when(idGenerator.generate("wd")).thenReturn("wd_manual");

        when(eventRepository.save(any(WebhookEvent.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        when(deliveryRepository.findByEventIdAndEndpointId(any(), any()))
            .thenReturn(Optional.empty());

        when(deliveryRepository.save(any(WebhookDelivery.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        WebhookOutboxService service = new WebhookOutboxService(
            merchantRepository,
            endpointRepository,
            eventRepository,
            deliveryRepository,
            idGenerator,
            jsonMapper
        );

        WebhookEvent event = service.enqueuePaymentManuallyAccepted(accepted);

        assertThat(event.getPublicId()).isEqualTo("evt_manual");
        assertThat(event.getType()).isEqualTo("payment.manually_accepted");
        assertThat(event.getPayloadJson()).contains("\"payment_id\":\"pay_manual\"");
        assertThat(event.getPayloadJson()).contains("\"status\":\"PENDING\"");
        assertThat(event.getPayloadJson()).contains("\"effective_status\":\"MANUALLY_ACCEPTED\"");
        assertThat(event.getPayloadJson()).contains("\"manually_accepted_at\"");

        ArgumentCaptor<WebhookDelivery> deliveryCaptor =
            ArgumentCaptor.forClass(WebhookDelivery.class);

        verify(deliveryRepository).save(deliveryCaptor.capture());

        assertThat(deliveryCaptor.getValue().getPublicId()).isEqualTo("wd_manual");
        assertThat(deliveryCaptor.getValue().getStatus().name()).isEqualTo("PENDING");
    }
}
