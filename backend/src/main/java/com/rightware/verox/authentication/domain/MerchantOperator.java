package com.rightware.verox.authentication.domain;

import com.rightware.verox.merchant.domain.Merchant;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "merchant_operators")
public class MerchantOperator {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ApiKeyEnvironment environment;
    @Column(nullable = false, unique = true, length = 160)
    private String username;
    @Column(name = "display_name", nullable = false, length = 160)
    private String displayName;
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MerchantOperatorStatus status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected MerchantOperator() {}

    public MerchantOperator(
        Merchant merchant,
        ApiKeyEnvironment environment,
        String username,
        String displayName,
        String passwordHash
    ) {
        this.id = UUID.randomUUID();
        this.merchant = merchant;
        this.environment = environment;
        this.username = username.trim().toLowerCase(Locale.ROOT);
        this.displayName = displayName.trim();
        this.passwordHash = passwordHash;
        this.status = MerchantOperatorStatus.ACTIVE;
    }

    @PrePersist void prePersist() {
        Instant now = Instant.now();
        if (id == null) id = UUID.randomUUID();
        if (status == null) status = MerchantOperatorStatus.ACTIVE;
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public Merchant getMerchant() { return merchant; }
    public ApiKeyEnvironment getEnvironment() { return environment; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public String getPasswordHash() { return passwordHash; }
    public MerchantOperatorStatus getStatus() { return status; }
    public boolean isActive() { return status == MerchantOperatorStatus.ACTIVE; }
}
