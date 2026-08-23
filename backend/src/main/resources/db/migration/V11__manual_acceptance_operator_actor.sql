ALTER TABLE pilot_manual_payment_acceptances
    ALTER COLUMN accepted_by_api_key_id DROP NOT NULL;

ALTER TABLE pilot_manual_payment_acceptances
    ADD COLUMN accepted_by_operator_id UUID REFERENCES merchant_operators(id),
    ADD COLUMN accepted_by_actor_type VARCHAR(32) NOT NULL DEFAULT 'API_KEY';

ALTER TABLE pilot_manual_payment_acceptances
    ADD CONSTRAINT chk_pilot_manual_acceptance_actor
    CHECK (
        (accepted_by_actor_type = 'API_KEY' AND accepted_by_api_key_id IS NOT NULL AND accepted_by_operator_id IS NULL)
        OR
        (accepted_by_actor_type = 'OPERATOR' AND accepted_by_operator_id IS NOT NULL AND accepted_by_api_key_id IS NULL)
    );
