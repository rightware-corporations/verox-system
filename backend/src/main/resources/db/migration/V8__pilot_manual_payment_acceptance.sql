CREATE TABLE pilot_manual_payment_acceptances (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL UNIQUE REFERENCES payments(id),
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    accepted_by_api_key_id UUID NOT NULL REFERENCES api_keys(id),
    reason VARCHAR(255),
    accepted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pilot_manual_acceptances_merchant_created
    ON pilot_manual_payment_acceptances(merchant_id, created_at DESC);
