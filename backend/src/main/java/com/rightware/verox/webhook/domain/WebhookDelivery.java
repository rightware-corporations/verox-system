package com.rightware.verox.webhook.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_deliveries")
public class WebhookDelivery {

    @Id
    private UUID id;

    @Column(name = "public_id", nullable = false, unique = true, length = 64)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private WebhookEvent event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "endpoint_id", nullable = false)
    private WebhookEndpoint endpoint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private WebhookDeliveryStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "last_status_code")
    private Integer lastStatusCode;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WebhookDelivery() {
    }

    public WebhookDelivery(String publicId, WebhookEvent event, WebhookEndpoint endpoint, Instant nextAttemptAt) {
        this.id = UUID.randomUUID();
        this.publicId = publicId;
        this.event = event;
        this.endpoint = endpoint;
        this.status = WebhookDeliveryStatus.PENDING;
        this.nextAttemptAt = nextAttemptAt == null ? Instant.now() : nextAttemptAt;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null) id = UUID.randomUUID();
        if (status == null) status = WebhookDeliveryStatus.PENDING;
        if (nextAttemptAt == null) nextAttemptAt = now;
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isAttemptableAt(Instant now) {
        Instant effectiveNow = now == null ? Instant.now() : now;
        return (status == WebhookDeliveryStatus.PENDING || status == WebhookDeliveryStatus.FAILED)
            && !nextAttemptAt.isAfter(effectiveNow);
    }

    public void recordSuccess(int statusCode, Instant attemptedAt) {
        if (status == WebhookDeliveryStatus.SUCCEEDED) {
            return;
        }
        if (status == WebhookDeliveryStatus.EXHAUSTED) {
            throw new IllegalStateException("Exhausted webhook delivery cannot succeed without a new delivery");
        }
        Instant effectiveAttemptedAt = attemptedAt == null ? Instant.now() : attemptedAt;
        attemptCount++;
        lastAttemptAt = effectiveAttemptedAt;
        lastStatusCode = statusCode;
        lastError = null;
        deliveredAt = effectiveAttemptedAt;
        nextAttemptAt = effectiveAttemptedAt;
        status = WebhookDeliveryStatus.SUCCEEDED;
    }

    public void recordFailure(
        Integer statusCode,
        String error,
        Instant attemptedAt,
        Instant retryAt,
        int maxAttempts
    ) {
        if (status == WebhookDeliveryStatus.SUCCEEDED) {
            throw new IllegalStateException("Succeeded webhook delivery cannot fail");
        }
        if (status == WebhookDeliveryStatus.EXHAUSTED) {
            return;
        }
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }

        Instant effectiveAttemptedAt = attemptedAt == null ? Instant.now() : attemptedAt;
        attemptCount++;
        lastAttemptAt = effectiveAttemptedAt;
        lastStatusCode = statusCode;
        lastError = normalizeError(error);
        deliveredAt = null;

        if (attemptCount >= maxAttempts) {
            status = WebhookDeliveryStatus.EXHAUSTED;
            nextAttemptAt = effectiveAttemptedAt;
            return;
        }

        if (retryAt == null || !retryAt.isAfter(effectiveAttemptedAt)) {
            throw new IllegalArgumentException("retryAt must be after attemptedAt for a retryable failure");
        }
        status = WebhookDeliveryStatus.FAILED;
        nextAttemptAt = retryAt;
    }

    private String normalizeError(String error) {
        if (error == null || error.isBlank()) {
            return "WEBHOOK_DELIVERY_FAILED";
        }
        String normalized = error.trim();
        return normalized.length() <= 2000 ? normalized : normalized.substring(0, 2000);
    }

    public UUID getId() { return id; }
    public String getPublicId() { return publicId; }
    public WebhookEvent getEvent() { return event; }
    public WebhookEndpoint getEndpoint() { return endpoint; }
    public WebhookDeliveryStatus getStatus() { return status; }
    public int getAttemptCount() { return attemptCount; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public Instant getLastAttemptAt() { return lastAttemptAt; }
    public Integer getLastStatusCode() { return lastStatusCode; }
    public String getLastError() { return lastError; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
