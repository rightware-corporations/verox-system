package com.rightware.verox.pilot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pilot_manual_payment_acceptances")
public class PilotManualPaymentAcceptance {

    @Id
    private UUID id;

    @Column(name = "payment_id", nullable = false, unique = true)
    private UUID paymentId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "accepted_by_api_key_id")
    private UUID acceptedByApiKeyId;

    @Column(name = "accepted_by_operator_id")
    private UUID acceptedByOperatorId;

    @Column(name = "accepted_by_actor_type", nullable = false, length = 32)
    private String acceptedByActorType;

    @Column(length = 255)
    private String reason;

    @Column(name = "accepted_at", nullable = false)
    private Instant acceptedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PilotManualPaymentAcceptance() {
    }

    public PilotManualPaymentAcceptance(
        UUID paymentId,
        UUID merchantId,
        UUID acceptedByApiKeyId,
        String reason
    ) {
        this(paymentId, merchantId, acceptedByApiKeyId, null, "API_KEY", reason);
    }

    public static PilotManualPaymentAcceptance acceptedByOperator(
        UUID paymentId,
        UUID merchantId,
        UUID operatorId,
        String reason
    ) {
        return new PilotManualPaymentAcceptance(paymentId, merchantId, null, operatorId, "OPERATOR", reason);
    }

    private PilotManualPaymentAcceptance(
        UUID paymentId,
        UUID merchantId,
        UUID acceptedByApiKeyId,
        UUID acceptedByOperatorId,
        String acceptedByActorType,
        String reason
    ) {
        this.id = UUID.randomUUID();
        this.paymentId = paymentId;
        this.merchantId = merchantId;
        this.acceptedByApiKeyId = acceptedByApiKeyId;
        this.acceptedByOperatorId = acceptedByOperatorId;
        this.acceptedByActorType = acceptedByActorType;
        this.reason = reason == null || reason.isBlank() ? null : reason.trim();
        this.acceptedAt = Instant.now();
    }

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (acceptedByActorType == null) acceptedByActorType = acceptedByOperatorId == null ? "API_KEY" : "OPERATOR";
        Instant now = Instant.now();
        if (acceptedAt == null) acceptedAt = now;
        if (createdAt == null) createdAt = now;
    }

    public UUID getId() { return id; }
    public UUID getPaymentId() { return paymentId; }
    public UUID getMerchantId() { return merchantId; }
    public UUID getAcceptedByApiKeyId() { return acceptedByApiKeyId; }
    public UUID getAcceptedByOperatorId() { return acceptedByOperatorId; }
    public String getAcceptedByActorType() { return acceptedByActorType; }
    public String getReason() { return reason; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
