CREATE TABLE bridges (
    id UUID PRIMARY KEY,
    public_id VARCHAR(64) NOT NULL UNIQUE,
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    name VARCHAR(160) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_bridges_merchant_name UNIQUE (merchant_id, name)
);

CREATE INDEX idx_bridges_merchant
    ON bridges(merchant_id);

CREATE TABLE bridge_credentials (
    id UUID PRIMARY KEY,
    bridge_id UUID NOT NULL REFERENCES bridges(id),
    key_prefix VARCHAR(32) NOT NULL,
    key_hash VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL,
    last_used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_bridge_credentials_bridge
    ON bridge_credentials(bridge_id);

CREATE INDEX idx_bridge_credentials_prefix
    ON bridge_credentials(key_prefix);
