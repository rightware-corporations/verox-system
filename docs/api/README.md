# VEROX Public API — MVP

## Authentication

Merchant API requests use a VEROX API key:

```http
Authorization: Bearer vx_live_xxxxxxxxxxxxxxxxxxxxxxxxx
```

Test keys use the `vx_test_` prefix and live keys use `vx_live_`. Only the SHA-256 hash of the full high-entropy key is stored by VEROX; the raw key is shown only when it is issued.

Current authenticated endpoint:

- `GET /v1/account` — returns the merchant resolved from the API key.

## Create Checkout Session

```http
POST /v1/checkout/sessions
Authorization: Bearer vx_live_xxxxx
Idempotency-Key: ORDER-82921
Content-Type: application/json
```

```json
{
  "amount": "1500.00",
  "currency": "MZN",
  "external_reference": "ORDER-82921",
  "description": "VIP Ticket",
  "success_url": "https://evento.example/payment/success",
  "cancel_url": "https://evento.example/payment/cancel"
}
```

Response:

```json
{
  "id": "cs_xxxxxxxxx",
  "payment_id": "pay_xxxxxxxxx",
  "external_reference": "ORDER-82921",
  "status": "OPEN",
  "payment_status": "PENDING",
  "amount": "1500.00",
  "currency": "MZN",
  "description": "VIP Ticket",
  "checkout_url": "https://checkout.verox.example/c/cs_xxxxxxxxx",
  "expires_at": "2026-08-08T22:30:00Z"
}
```

The same merchant and `Idempotency-Key` return the same Checkout Session when the request parameters are identical. Reusing the key with different parameters returns `409 IDEMPOTENCY_CONFLICT`.

VEROX MVP currently accepts `MZN`. Internally amounts are persisted in integer minor units; floating-point storage is not used.

## Retrieve Checkout Session

```http
GET /v1/checkout/sessions/{checkout_session_id}
Authorization: Bearer vx_live_xxxxx
```

Resources are merchant-scoped. A merchant cannot retrieve another merchant's Checkout Session.

## Retrieve Payment

```http
GET /v1/payments/{payment_id}
Authorization: Bearer vx_live_xxxxx
```

Payment response includes the Checkout Session reference, merchant external reference, status, amount, currency and provider when available.

## Hosted checkout

Next implementation phase:

- public Checkout Session retrieval for `frontend/checkout`
- `POST /public/v1/checkout/{checkoutSessionId}/evidence`

## Bridge

Planned MVP bridge endpoint:

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
