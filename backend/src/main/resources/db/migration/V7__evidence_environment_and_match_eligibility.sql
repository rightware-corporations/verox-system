ALTER TABLE bridges
    ADD COLUMN environment VARCHAR(16);

UPDATE bridges
SET environment = 'TEST'
WHERE environment IS NULL;

ALTER TABLE bridges
    ALTER COLUMN environment SET NOT NULL;

ALTER TABLE bridges
    ADD CONSTRAINT ck_bridges_environment
    CHECK (environment IN ('TEST', 'LIVE'));

CREATE INDEX idx_bridges_merchant_environment
    ON bridges(merchant_id, environment);

ALTER TABLE evidences
    ADD COLUMN environment VARCHAR(16);

UPDATE evidences e
SET environment = p.environment
FROM payments p
WHERE e.payment_id = p.id
  AND e.environment IS NULL;

-- Existing unlinked Provider Evidence predates environment-aware Bridges.
-- The current pre-production database contains development/test evidence only.
UPDATE evidences
SET environment = 'TEST'
WHERE environment IS NULL;

ALTER TABLE evidences
    ALTER COLUMN environment SET NOT NULL;

ALTER TABLE evidences
    ADD CONSTRAINT ck_evidences_environment
    CHECK (environment IN ('TEST', 'LIVE'));

DROP INDEX uq_provider_evidence_merchant_content;

CREATE UNIQUE INDEX uq_provider_evidence_merchant_environment_content
    ON evidences(merchant_id, environment, origin, kind, content_sha256)
    WHERE origin = 'PROVIDER';

DROP INDEX idx_evidences_unlinked_provider_received;

CREATE INDEX idx_evidences_unlinked_provider_match_eligibility
    ON evidences(merchant_id, environment, provider, created_at ASC)
    WHERE origin = 'PROVIDER' AND payment_id IS NULL;

DROP INDEX uq_payments_merchant_provider_reference;

CREATE UNIQUE INDEX uq_payments_merchant_environment_provider_reference
    ON payments(merchant_id, environment, provider, provider_transaction_reference)
    WHERE provider IS NOT NULL
      AND provider_transaction_reference IS NOT NULL;
