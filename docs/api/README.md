# VEROX Public API — MVP

Planned public endpoints:

- `POST /v1/checkout/sessions`
- `GET /v1/checkout/sessions/{id}`
- `GET /v1/payments/{id}`

Planned public checkout endpoint:

- `POST /public/v1/checkout/{publicToken}/evidence`

Planned bridge endpoint:

- `POST /v1/bridges/{bridgeId}/evidence`

The merchant receives payment results through signed webhooks. Redirect URLs are a user-experience mechanism and are not authoritative payment confirmation.
