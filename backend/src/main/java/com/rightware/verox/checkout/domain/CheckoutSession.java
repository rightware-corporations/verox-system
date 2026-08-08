package com.rightware.verox.checkout.domain;

import com.rightware.verox.authentication.domain.ApiKeyEnvironment;
import com.rightware.verox.merchant.domain.Merchant;
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
@Table(name = "checkout_sessions")
public class CheckoutSession {

    @Id
    private UUID id;

    @Column(name = "public_id", nullable = false, unique = true, length = 64)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ApiKeyEnvironment environment;

    @Column(name = "external_reference", nullable = false, length = 160)
    private String externalReference;

    @Column(length = 255)
    private String description;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "success_url", nullable = false, columnDefinition = "TEXT")
    private String successUrl;

    @Column(name = "cancel_url", nullable = false, columnDefinition = "TEXT")
    private String cancelUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CheckoutSessionStatus status;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "request_fingerprint", nullable = false, length = 64)
    private String requestFingerprint;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CheckoutSession() {
    }

    public CheckoutSession(
        String publicId,
        Merchant merchant,
        ApiKeyEnvironment environment,
        String externalReference,
        String description,
        long amountMinor,
        String currency,
        String successUrl,
        String cancelUrl,
        String idempotencyKey,
        String requestFingerprint,
        Instant expiresAt
    ) {
        this.id = UUID.randomUUID();
        this.publicId = publicId;
        this.merchant = merchant;
        this.environment = environment;
        this.externalReference = externalReference;
        this.description = description;
        this.amountMinor = amountMinor;
        this.currency = currency;
        this.successUrl = successUrl;
        this.cancelUrl = cancelUrl;
        this.status = CheckoutSessionStatus.OPEN;
        this.idempotencyKey = idempotencyKey;
        this.requestFingerprint = requestFingerprint;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = CheckoutSessionStatus.OPEN;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getPublicId() {
        return publicId;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public ApiKeyEnvironment getEnvironment() {
        return environment;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public String getDescription() {
        return description;
    }

    public long getAmountMinor() {
        return amountMinor;
    }

    public String getCurrency() {
        return currency;
    }

    public String getSuccessUrl() {
        return successUrl;
    }

    public String getCancelUrl() {
        return cancelUrl;
    }

    public CheckoutSessionStatus getStatus() {
        return status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
