# VEROX Public API — MVP

## Authentication

Merchant API requests use a VEROX API key:

```http
Authorization: Bearer vx_live_xxxxxxxxxxxxxxxxxxxxxxxxx
```

Test keys use the `vx_test_` prefix and live keys use `vx_live_`. Only the hash of the full key is stored by VEROX; the raw key is shown only when it is issued.

Current authenticated endpoint:

- `GET /v1/account` — returns the merchant resolved from the API key.

## Checkout and payments

Planned public merchant endpoints:

- `POST /v1/checkout/sessions`
- `GET /v1/checkout/sessions/{id}`
- `GET /v1/payments/{id}`

Planned hosted checkout endpoint:

- `POST /public/v1/checkout/{publicToken}/evidence`

Planned bridge endpoint:

- `POST /v1/bridges/{bridgeId}/evidence`

The merchant receives payment results through signed webhooks. Redirect URLs are a user-experience mechanism and are not authoritative payment confirmation.

## MVP merchant bootstrap

Until the merchant dashboard exists, a first merchant can be provisioned by starting the backend once with:

```text
VEROX_BOOTSTRAP_ENABLED=true
VEROX_BOOTSTRAP_MERCHANT_NAME=<merchant name>
VEROX_BOOTSTRAP_ENVIRONMENT=TEST|LIVE
```

VEROX creates the merchant and prints the generated API key once. Copy it to a secure secrets store and immediately disable bootstrap afterwards.
