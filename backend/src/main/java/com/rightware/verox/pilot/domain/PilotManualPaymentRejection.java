package com.rightware.verox.pilot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pilot_manual_payment_rejections")
public class PilotManualPaymentRejection {
    @Id private UUID id;
    @Column(name = "payment_id", nullable = false, unique = true) private UUID paymentId;
    @Column(name = "merchant_id", nullable = false) private UUID merchantId;
    @Column(name = "rejected_by_api_key_id") private UUID rejectedByApiKeyId;
    @Column(name = "rejected_by_operator_id") private UUID rejectedByOperatorId;
    @Column(name = "rejected_by_actor_type", nullable = false, length = 32) private String rejectedByActorType;
    @Column(length = 255) private String reason;
    @Column(name = "rejected_at", nullable = false) private Instant rejectedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected PilotManualPaymentRejection() {}
    public PilotManualPaymentRejection(UUID paymentId, UUID merchantId, UUID apiKeyId, UUID operatorId, String reason) {
        this.id = UUID.randomUUID(); this.paymentId = paymentId; this.merchantId = merchantId;
        this.rejectedByApiKeyId = apiKeyId; this.rejectedByOperatorId = operatorId;
        this.rejectedByActorType = operatorId == null ? "API_KEY" : "OPERATOR";
        this.reason = reason == null || reason.isBlank() ? null : reason.trim();
        this.rejectedAt = Instant.now();
    }
    @PrePersist void prePersist() { if (id == null) id = UUID.randomUUID(); if (rejectedAt == null) rejectedAt = Instant.now(); if (createdAt == null) createdAt = Instant.now(); }
    public UUID getId() { return id; }
    public UUID getPaymentId() { return paymentId; }
    public UUID getMerchantId() { return merchantId; }
    public UUID getRejectedByOperatorId() { return rejectedByOperatorId; }
    public String getReason() { return reason; }
    public Instant getRejectedAt() { return rejectedAt; }
}
