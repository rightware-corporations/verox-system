CREATE TABLE merchant_operators (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    environment VARCHAR(16) NOT NULL,
    username VARCHAR(160) NOT NULL UNIQUE,
    display_name VARCHAR(160) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_merchant_operators_merchant_environment
    ON merchant_operators(merchant_id, environment);

CREATE TABLE merchant_operator_sessions (
    id UUID PRIMARY KEY,
    operator_id UUID NOT NULL REFERENCES merchant_operators(id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    csrf_token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    last_seen_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_merchant_operator_sessions_operator
    ON merchant_operator_sessions(operator_id);

CREATE INDEX idx_merchant_operator_sessions_expiry
    ON merchant_operator_sessions(expires_at);
