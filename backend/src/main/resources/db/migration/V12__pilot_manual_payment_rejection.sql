CREATE TABLE pilot_manual_payment_rejections (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL UNIQUE REFERENCES payments(id),
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    rejected_by_api_key_id UUID REFERENCES api_keys(id),
    rejected_by_operator_id UUID,
    rejected_by_actor_type VARCHAR(32) NOT NULL,
    reason VARCHAR(255),
    rejected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pilot_manual_rejections_merchant_created
    ON pilot_manual_payment_rejections(merchant_id, created_at DESC);

CREATE INDEX idx_pilot_manual_rejections_payment_merchant
    ON pilot_manual_payment_rejections(payment_id, merchant_id);
