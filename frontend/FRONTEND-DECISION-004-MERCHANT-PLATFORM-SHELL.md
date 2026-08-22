# FRONTEND-DECISION-004 — Merchant Platform Shell and Data Boundary

Status: APPROVED FRONTEND DECISION

## Decision

The Merchant Platform is implemented as a separate browser surface under `frontend/dashboard/`, with a dark VEROX operational shell and responsive routes rooted at `/app/*`.

Initial routes:

- `/app/overview`
- `/app/payments`
- `/app/developers`
- `/app/api-keys`
- `/app/webhooks`
- `/app/settings`

## Product boundary

The Merchant Platform must not fabricate payment truth, aggregate metrics or resource collections that the Merchant API does not expose.

Current inspected Merchant API supports:

- `GET /v1/account`
- `POST /v1/checkout/sessions`
- `GET /v1/checkout/sessions/{checkoutSessionId}`
- `GET /v1/payments/{paymentId}`
- webhook endpoint configuration already present in backend project status

At the time of this decision there is no merchant-scoped payment collection endpoint and no aggregate dashboard metrics endpoint.

Therefore Overview uses explicit unavailable placeholders rather than fake production numbers. Payments remains a prepared surface until a server-authoritative collection contract is approved.

## Visual direction

Hosted Checkout remains light-first and transactional.
Merchant Platform is dark-first and operational.
Both share VEROX principles: simple experience, strict truth, clear ownership.

## Security

No Merchant API key is embedded in frontend source. Authentication/session design remains a separate contract decision; merchant secret API keys are server credentials and must not be exposed to browser bundles.

## Next implementation sequence

1. connect safe merchant account/session authentication contract;
2. implement Payments collection after backend contract review;
3. Payment Detail from real `GET /v1/payments/{paymentId}` data;
4. webhook operations;
5. developer/API-key surfaces only after browser-safe management contracts exist.
