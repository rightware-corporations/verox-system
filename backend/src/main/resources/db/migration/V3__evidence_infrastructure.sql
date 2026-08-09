CREATE TABLE evidences (
    id UUID PRIMARY KEY,
    public_id VARCHAR(64) NOT NULL UNIQUE,
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    payment_id UUID NOT NULL REFERENCES payments(id),
    origin VARCHAR(16) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    ingest_source VARCHAR(32) NOT NULL,
    provider VARCHAR(32),
    content_sha256 VARCHAR(64) NOT NULL,
    content_type VARCHAR(128),
    original_filename VARCHAR(255),
    storage_key TEXT,
    raw_content TEXT,
    occurred_at TIMESTAMPTZ,
    received_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_evidence_payment_content UNIQUE (payment_id, origin, kind, content_sha256),
    CONSTRAINT ck_evidence_payload_present CHECK (storage_key IS NOT NULL OR raw_content IS NOT NULL)
);

CREATE INDEX idx_evidences_payment_received
    ON evidences(payment_id, received_at DESC);

CREATE INDEX idx_evidences_merchant_received
    ON evidences(merchant_id, received_at DESC);

CREATE INDEX idx_evidences_provider
    ON evidences(provider)
    WHERE provider IS NOT NULL;

CREATE INDEX idx_evidences_content_hash
    ON evidences(content_sha256);
