package com.rightware.verox.webhook.application;

import com.rightware.verox.common.id.ResourceIdGenerator;
import com.rightware.verox.merchant.domain.Merchant;
import com.rightware.verox.merchant.repository.MerchantRepository;
import com.rightware.verox.payment.application.PaymentConfirmedEvent;
import com.rightware.verox.pilot.application.PaymentManuallyAcceptedEvent;
import com.rightware.verox.pilot.application.PaymentManuallyRejectedEvent;
import com.rightware.verox.webhook.domain.WebhookDelivery;
import com.rightware.verox.webhook.domain.WebhookEndpoint;
import com.rightware.verox.webhook.domain.WebhookEndpointStatus;
import com.rightware.verox.webhook.domain.WebhookEvent;
import com.rightware.verox.webhook.repository.WebhookDeliveryRepository;
import com.rightware.verox.webhook.repository.WebhookEndpointRepository;
import com.rightware.verox.webhook.repository.WebhookEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class WebhookOutboxService {

    private static final String PAYMENT_CONFIRMED = "payment.confirmed";
    private static final String PAYMENT_MANUALLY_ACCEPTED = "payment.manually_accepted";
    private static final String PAYMENT_MANUALLY_REJECTED = "payment.manually_rejected";

    private final MerchantRepository merchantRepository;
    private final WebhookEndpointRepository endpointRepository;
    private final WebhookEventRepository eventRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final ResourceIdGenerator resourceIdGenerator;
    private final JsonMapper jsonMapper;

    public WebhookOutboxService(
        MerchantRepository merchantRepository,
        WebhookEndpointRepository endpointRepository,
        WebhookEventRepository eventRepository,
        WebhookDeliveryRepository deliveryRepository,
        ResourceIdGenerator resourceIdGenerator,
        JsonMapper jsonMapper
    ) {
        this.merchantRepository = merchantRepository;
        this.endpointRepository = endpointRepository;
        this.eventRepository = eventRepository;
        this.deliveryRepository = deliveryRepository;
        this.resourceIdGenerator = resourceIdGenerator;
        this.jsonMapper = jsonMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WebhookEvent enqueuePaymentConfirmed(PaymentConfirmedEvent confirmed) {
        WebhookEvent existing = eventRepository
            .findByMerchantIdAndTypeAndAggregateTypeAndAggregatePublicId(
                confirmed.merchantId(), PAYMENT_CONFIRMED, "PAYMENT", confirmed.paymentId()
            )
            .orElse(null);
        if (existing != null) {
            ensureDelivery(existing, confirmed.merchantId());
            return existing;
        }

        Merchant merchant = merchantRepository.findById(confirmed.merchantId())
            .orElseThrow(() -> new IllegalStateException("Merchant was not found for webhook event"));

        Instant createdAt = confirmed.confirmedAt() == null ? Instant.now() : confirmed.confirmedAt();
        String eventPublicId = resourceIdGenerator.generate("evt");
        String payload = serializePayload(eventPublicId, createdAt, confirmed);

        WebhookEvent event = eventRepository.save(new WebhookEvent(
            eventPublicId,
            merchant,
            PAYMENT_CONFIRMED,
            "PAYMENT",
            confirmed.paymentId(),
            payload,
            createdAt
        ));
        ensureDelivery(event, confirmed.merchantId());
        return event;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WebhookEvent enqueuePaymentManuallyAccepted(PaymentManuallyAcceptedEvent accepted) {
        WebhookEvent existing = eventRepository
            .findByMerchantIdAndTypeAndAggregateTypeAndAggregatePublicId(
                accepted.merchantId(),
                PAYMENT_MANUALLY_ACCEPTED,
                "PAYMENT",
                accepted.paymentId()
            )
            .orElse(null);

        if (existing != null) {
            ensureDelivery(existing, accepted.merchantId());
            return existing;
        }

        Merchant merchant = merchantRepository.findById(accepted.merchantId())
            .orElseThrow(() -> new IllegalStateException(
                "Merchant was not found for webhook event"
            ));

        Instant createdAt = accepted.manuallyAcceptedAt() == null
            ? Instant.now()
            : accepted.manuallyAcceptedAt();

        String eventPublicId = resourceIdGenerator.generate("evt");
        String payload = serializeManualAcceptancePayload(
            eventPublicId,
            createdAt,
            accepted
        );

        WebhookEvent event = eventRepository.save(new WebhookEvent(
            eventPublicId,
            merchant,
            PAYMENT_MANUALLY_ACCEPTED,
            "PAYMENT",
            accepted.paymentId(),
            payload,
            createdAt
        ));

        ensureDelivery(event, accepted.merchantId());
        return event;
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WebhookEvent enqueuePaymentManuallyRejected(PaymentManuallyRejectedEvent rejected) {
        WebhookEvent existing = eventRepository.findByMerchantIdAndTypeAndAggregateTypeAndAggregatePublicId(rejected.merchantId(), PAYMENT_MANUALLY_REJECTED, "PAYMENT", rejected.paymentId()).orElse(null);
        if (existing != null) { ensureDelivery(existing, rejected.merchantId()); return existing; }
        Merchant merchant = merchantRepository.findById(rejected.merchantId()).orElseThrow(() -> new IllegalStateException("Merchant was not found for webhook event"));
        Instant createdAt = rejected.manuallyRejectedAt() == null ? Instant.now() : rejected.manuallyRejectedAt();
        String eventPublicId = resourceIdGenerator.generate("evt");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("payment_id", rejected.paymentId()); data.put("checkout_session_id", rejected.checkoutSessionId()); data.put("external_reference", rejected.externalReference()); data.put("amount", rejected.amount()); data.put("currency", rejected.currency()); data.put("status", rejected.status()); data.put("effective_status", "MANUALLY_REJECTED"); data.put("manually_rejected_at", rejected.manuallyRejectedAt());
        Map<String, Object> payload = new LinkedHashMap<>(); payload.put("id", eventPublicId); payload.put("type", PAYMENT_MANUALLY_REJECTED); payload.put("created_at", createdAt); payload.put("data", data);
        try {
            WebhookEvent event = eventRepository.save(new WebhookEvent(eventPublicId, merchant, PAYMENT_MANUALLY_REJECTED, "PAYMENT", rejected.paymentId(), jsonMapper.writeValueAsString(payload), createdAt));
            ensureDelivery(event, rejected.merchantId()); return event;
        } catch (JacksonException exception) { throw new IllegalStateException("Unable to serialize webhook event payload", exception); }
    }

    private void ensureDelivery(WebhookEvent event, java.util.UUID merchantId) {
        WebhookEndpoint endpoint = endpointRepository.findByMerchantIdAndStatus(merchantId, WebhookEndpointStatus.ACTIVE)
            .orElse(null);
        if (endpoint == null) {
            return;
        }
        if (deliveryRepository.findByEventIdAndEndpointId(event.getId(), endpoint.getId()).isPresent()) {
            return;
        }
        deliveryRepository.save(new WebhookDelivery(
            resourceIdGenerator.generate("wd"),
            event,
            endpoint,
            Instant.now()
        ));
    }

    private String serializeManualAcceptancePayload(
        String eventId,
        Instant createdAt,
        PaymentManuallyAcceptedEvent accepted
    ) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("payment_id", accepted.paymentId());
        data.put("checkout_session_id", accepted.checkoutSessionId());
        data.put("external_reference", accepted.externalReference());
        data.put("amount", accepted.amount());
        data.put("currency", accepted.currency());
        data.put("status", accepted.status());
        data.put("effective_status", "MANUALLY_ACCEPTED");
        data.put("manually_accepted_at", accepted.manuallyAcceptedAt());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", eventId);
        payload.put("type", PAYMENT_MANUALLY_ACCEPTED);
        payload.put("created_at", createdAt);
        payload.put("data", data);

        try {
            return jsonMapper.writeValueAsString(payload);
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                "Unable to serialize webhook event payload",
                exception
            );
        }
    }
    private String serializePayload(String eventId, Instant createdAt, PaymentConfirmedEvent confirmed) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("payment_id", confirmed.paymentId());
        data.put("checkout_session_id", confirmed.checkoutSessionId());
        data.put("external_reference", confirmed.externalReference());
        data.put("amount", confirmed.amount());
        data.put("currency", confirmed.currency());
        data.put("provider", confirmed.provider());
        data.put("provider_transaction_reference", confirmed.providerTransactionReference());
        data.put("status", "CONFIRMED");
        data.put("confirmed_at", confirmed.confirmedAt());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", eventId);
        payload.put("type", PAYMENT_CONFIRMED);
        payload.put("created_at", createdAt);
        payload.put("data", data);

        try {
            return jsonMapper.writeValueAsString(payload);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Unable to serialize webhook event payload", exception);
        }
    }
}
