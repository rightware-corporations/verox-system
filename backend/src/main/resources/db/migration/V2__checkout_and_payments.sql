CREATE TABLE checkout_sessions (
    id UUID PRIMARY KEY,
    public_id VARCHAR(64) NOT NULL UNIQUE,
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    environment VARCHAR(16) NOT NULL,
    external_reference VARCHAR(160) NOT NULL,
    description VARCHAR(255),
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    currency VARCHAR(3) NOT NULL,
    success_url TEXT NOT NULL,
    cancel_url TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_checkout_sessions_merchant_idempotency UNIQUE (merchant_id, idempotency_key)
);

CREATE INDEX idx_checkout_sessions_merchant_reference
    ON checkout_sessions(merchant_id, external_reference);
CREATE INDEX idx_checkout_sessions_merchant_created
    ON checkout_sessions(merchant_id, created_at DESC);

CREATE TABLE payments (
    id UUID PRIMARY KEY,
    public_id VARCHAR(64) NOT NULL UNIQUE,
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    checkout_session_id UUID NOT NULL UNIQUE REFERENCES checkout_sessions(id),
    environment VARCHAR(16) NOT NULL,
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    currency VARCHAR(3) NOT NULL,
    provider VARCHAR(32),
    provider_transaction_reference VARCHAR(128),
    status VARCHAR(32) NOT NULL,
    confirmed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_merchant_created
    ON payments(merchant_id, created_at DESC);
CREATE INDEX idx_payments_merchant_status
    ON payments(merchant_id, status);
CREATE INDEX idx_payments_provider_reference
    ON payments(provider, provider_transaction_reference)
    WHERE provider_transaction_reference IS NOT NULL;
