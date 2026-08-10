CREATE UNIQUE INDEX uq_payments_merchant_provider_reference
    ON payments(merchant_id, provider, provider_transaction_reference)
    WHERE provider IS NOT NULL
      AND provider_transaction_reference IS NOT NULL;
