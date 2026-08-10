package com.rightware.verox.webhook.domain;

import com.rightware.verox.merchant.domain.Merchant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_events")
public class WebhookEvent {

    @Id
    private UUID id;

    @Column(name = "public_id", nullable = false, unique = true, length = 64)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(nullable = false, length = 64)
    private String type;

    @Column(name = "aggregate_type", nullable = false, length = 32)
    private String aggregateType;

    @Column(name = "aggregate_public_id", nullable = false, length = 64)
    private String aggregatePublicId;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WebhookEvent() {
    }

    public WebhookEvent(
        String publicId,
        Merchant merchant,
        String type,
        String aggregateType,
        String aggregatePublicId,
        String payloadJson,
        Instant createdAt
    ) {
        this.id = UUID.randomUUID();
        this.publicId = publicId;
        this.merchant = merchant;
        this.type = type;
        this.aggregateType = aggregateType;
        this.aggregatePublicId = aggregatePublicId;
        this.payloadJson = payloadJson;
        this.createdAt = createdAt;
    }

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getPublicId() { return publicId; }
    public Merchant getMerchant() { return merchant; }
    public String getType() { return type; }
    public String getAggregateType() { return aggregateType; }
    public String getAggregatePublicId() { return aggregatePublicId; }
    public String getPayloadJson() { return payloadJson; }
    public Instant getCreatedAt() { return createdAt; }
}
