package com.rightware.verox.evidence.domain;

import com.rightware.verox.authentication.domain.ApiKeyEnvironment;
import com.rightware.verox.merchant.domain.Merchant;
import com.rightware.verox.payment.domain.Payment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "evidences")
public class Evidence {

    @Id
    private UUID id;

    @Column(name = "public_id", nullable = false, unique = true, length = 64)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ApiKeyEnvironment environment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EvidenceOrigin origin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EvidenceKind kind;

    @Enumerated(EnumType.STRING)
    @Column(name = "ingest_source", nullable = false, length = 32)
    private EvidenceIngestSource ingestSource;

    @Column(length = 32)
    private String provider;

    @Column(name = "content_sha256", nullable = false, length = 64)
    private String contentSha256;

    @Column(name = "content_type", length = 128)
    private String contentType;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "storage_key", columnDefinition = "TEXT")
    private String storageKey;

    @Column(name = "raw_content", columnDefinition = "TEXT")
    private String rawContent;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "linked_at")
    private Instant linkedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Evidence() {
    }

    public Evidence(
        String publicId,
        Merchant merchant,
        Payment payment,
        ApiKeyEnvironment environment,
        EvidenceOrigin origin,
        EvidenceKind kind,
        EvidenceIngestSource ingestSource,
        String provider,
        String contentSha256,
        String contentType,
        String originalFilename,
        String storageKey,
        String rawContent,
        Instant occurredAt,
        Instant receivedAt
    ) {
        this.id = UUID.randomUUID();
        this.publicId = requireText(publicId, "publicId");
        this.merchant = Objects.requireNonNull(merchant, "merchant");
        this.payment = payment;
        this.environment = Objects.requireNonNull(environment, "environment");
        this.origin = Objects.requireNonNull(origin, "origin");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.ingestSource = Objects.requireNonNull(ingestSource, "ingestSource");
        this.provider = trimToNull(provider);
        this.contentSha256 = requireHash(contentSha256);
        this.contentType = trimToNull(contentType);
        this.originalFilename = trimToNull(originalFilename);
        this.storageKey = trimToNull(storageKey);
        this.rawContent = trimToNull(rawContent);
        this.occurredAt = occurredAt;
        this.receivedAt = receivedAt;

        if (this.storageKey == null && this.rawContent == null) {
            throw new IllegalArgumentException("Evidence requires storageKey or rawContent");
        }
        validatePaymentMerchantAndEnvironment(payment);
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (receivedAt == null) {
            receivedAt = now;
        }
        if (payment != null && linkedAt == null) {
            linkedAt = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
    }

    public void linkToPayment(Payment payment) {
        Objects.requireNonNull(payment, "payment");
        validatePaymentMerchantAndEnvironment(payment);
        if (this.payment != null && !this.payment.getId().equals(payment.getId())) {
            throw new IllegalStateException("Evidence is already linked to another Payment");
        }
        this.payment = payment;
        if (this.linkedAt == null) {
            this.linkedAt = Instant.now();
        }
    }

    private void validatePaymentMerchantAndEnvironment(Payment payment) {
        if (payment == null) {
            return;
        }
        Merchant paymentMerchant = payment.getMerchant();
        if (paymentMerchant == null || paymentMerchant.getId() == null || merchant.getId() == null
            || !merchant.getId().equals(paymentMerchant.getId())) {
            throw new IllegalArgumentException("Evidence and Payment must belong to the same Merchant");
        }
        if (payment.getEnvironment() != environment) {
            throw new IllegalArgumentException("Evidence and Payment must belong to the same environment");
        }
    }

    private static String requireText(String value, String field) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    private static String requireHash(String value) {
        String hash = requireText(value, "contentSha256").toLowerCase();
        if (!hash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("contentSha256 must be a SHA-256 hex digest");
        }
        return hash;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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

    public Payment getPayment() {
        return payment;
    }

    public ApiKeyEnvironment getEnvironment() {
        return environment;
    }

    public EvidenceOrigin getOrigin() {
        return origin;
    }

    public EvidenceKind getKind() {
        return kind;
    }

    public EvidenceIngestSource getIngestSource() {
        return ingestSource;
    }

    public String getProvider() {
        return provider;
    }

    public String getContentSha256() {
        return contentSha256;
    }

    public String getContentType() {
        return contentType;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getRawContent() {
        return rawContent;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public Instant getLinkedAt() {
        return linkedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
