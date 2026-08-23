package com.rightware.verox.paymentchannel.domain;

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
@Table(name = "merchant_payment_channels")
public class MerchantPaymentChannel {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ApiKeyEnvironment environment;

    @Column(nullable = false, length = 32)
    private String provider;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(nullable = false, length = 64)
    private String kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentChannelStatus status;

    @Column(name = "recipient_display", length = 160)
    private String recipientDisplay;

    @Column(name = "recipient_name", length = 160)
    private String recipientName;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MerchantPaymentChannel() {
    }

    public MerchantPaymentChannel(
        Merchant merchant,
        ApiKeyEnvironment environment,
        String provider,
        String displayName,
        String kind,
        PaymentChannelStatus status,
        String recipientDisplay,
        String recipientName,
        String instructions
    ) {
        this.id = UUID.randomUUID();
        this.merchant = merchant;
        this.environment = environment;
        this.provider = provider;
        this.displayName = displayName;
        this.kind = kind;
        this.status = status;
        this.recipientDisplay = recipientDisplay;
        this.recipientName = recipientName;
        this.instructions = instructions;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = PaymentChannelStatus.INACTIVE;
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

    public Merchant getMerchant() {
        return merchant;
    }

    public ApiKeyEnvironment getEnvironment() {
        return environment;
    }

    public String getProvider() {
        return provider;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getKind() {
        return kind;
    }

    public PaymentChannelStatus getStatus() {
        return status;
    }

    public String getRecipientDisplay() {
        return recipientDisplay;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getInstructions() {
        return instructions;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isActive() {
        return status == PaymentChannelStatus.ACTIVE;
    }
}
