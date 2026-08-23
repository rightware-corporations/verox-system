package com.rightware.verox.authentication.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "merchant_operator_sessions")
public class MerchantOperatorSession {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operator_id", nullable = false)
    private MerchantOperator operator;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(name = "csrf_token_hash", nullable = false, length = 64)
    private String csrfTokenHash;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "revoked_at") private Instant revokedAt;
    @Column(name = "last_seen_at") private Instant lastSeenAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected MerchantOperatorSession() {}

    public MerchantOperatorSession(MerchantOperator operator, String tokenHash, String csrfTokenHash, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.operator = operator;
        this.tokenHash = tokenHash;
        this.csrfTokenHash = csrfTokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public boolean isActiveAt(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public void revoke(Instant now) { if (revokedAt == null) revokedAt = now; }
    public void touch(Instant now) { lastSeenAt = now; }

    public UUID getId() { return id; }
    public MerchantOperator getOperator() { return operator; }
    public String getTokenHash() { return tokenHash; }
    public String getCsrfTokenHash() { return csrfTokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public Instant getCreatedAt() { return createdAt; }
}
