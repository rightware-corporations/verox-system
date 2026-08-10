package com.rightware.verox.payment.domain;

import com.rightware.verox.authentication.domain.ApiKeyEnvironment;
import com.rightware.verox.checkout.domain.CheckoutSession;
import com.rightware.verox.merchant.domain.Merchant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private UUID id;

    @Column(name = "public_id", nullable = false, unique = true, length = 64)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "checkout_session_id", nullable = false, unique = true)
    private CheckoutSession checkoutSession;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ApiKeyEnvironment environment;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 32)
    private String provider;

    @Column(name = "provider_transaction_reference", length = 128)
    private String providerTransactionReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentStatus status;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Payment() {
    }

    public Payment(
        String publicId,
        Merchant merchant,
        CheckoutSession checkoutSession,
        ApiKeyEnvironment environment,
        long amountMinor,
        String currency
    ) {
        this.id = UUID.randomUUID();
        this.publicId = publicId;
        this.merchant = merchant;
        this.checkoutSession = checkoutSession;
        this.environment = environment;
        this.amountMinor = amountMinor;
        this.currency = currency;
        this.status = PaymentStatus.PENDING;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = PaymentStatus.PENDING;
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

    public void beginVerification() {
        if (status == PaymentStatus.PENDING) {
            status = PaymentStatus.VERIFYING;
            return;
        }
        if (status != PaymentStatus.VERIFYING) {
            throw new IllegalStateException("Payment cannot enter VERIFYING from " + status);
        }
    }

    public void requireReview() {
        if (status == PaymentStatus.REVIEW_REQUIRED) {
            return;
        }
        if (status != PaymentStatus.PENDING && status != PaymentStatus.VERIFYING) {
            throw new IllegalStateException("Payment cannot enter REVIEW_REQUIRED from " + status);
        }
        status = PaymentStatus.REVIEW_REQUIRED;
    }

    public void confirm(String provider, String transactionReference, Instant confirmedAt) {
        if (status != PaymentStatus.VERIFYING) {
            throw new IllegalStateException("Payment can only be confirmed from VERIFYING");
        }
        this.provider = normalizeRequired(provider, "provider");
        this.providerTransactionReference = normalizeRequired(transactionReference, "transactionReference");
        this.confirmedAt = confirmedAt == null ? Instant.now() : confirmedAt;
        this.status = PaymentStatus.CONFIRMED;
    }

    private String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim().toUpperCase(Locale.ROOT);
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

    public CheckoutSession getCheckoutSession() {
        return checkoutSession;
    }

    public ApiKeyEnvironment getEnvironment() {
        return environment;
    }

    public long getAmountMinor() {
        return amountMinor;
    }

    public String getCurrency() {
        return currency;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderTransactionReference() {
        return providerTransactionReference;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
