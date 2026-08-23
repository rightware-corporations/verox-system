CREATE TABLE merchant_payment_channels (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    environment VARCHAR(16) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    kind VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    recipient_display VARCHAR(160),
    recipient_name VARCHAR(160),
    instructions TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_merchant_payment_channels_scope_provider
        UNIQUE (merchant_id, environment, provider)
);

CREATE INDEX idx_merchant_payment_channels_scope_status
    ON merchant_payment_channels(merchant_id, environment, status);
