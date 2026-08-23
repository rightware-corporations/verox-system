# FRONTEND-DECISION-005 — Merchant Platform Pilot Operational Model

Status: APPROVED FRONTEND DECISION

The VEROX Merchant Platform preserves the existing VEROX visual system and is implemented using the strategy:

**REAL PILOT FIRST + TENANT-READY ARCHITECTURE UNDERNEATH.**

The browser never owns a VEROX Merchant API key. Merchant API credentials are server secrets. The current `frontend/dashboard` application is a browser-only Vite frontend and has no inspected BFF, server action, API route or secure proxy that can own the Merchant Bearer credential. Therefore authenticated `/v1/*` integrations remain intentionally blocked at a typed frontend service boundary until that server-owned boundary exists.

Current pilot presentation metadata is configuration-only:

- brand: Money Makers
- primary contact: Owen de Jesus

These values are never used for authorization, merchant ownership, API scoping or manual-acceptance enablement. Backend `merchantId` remains the ownership authority.

The frontend models only audited backend contracts:

- `GET /v1/account`
- `POST /v1/checkout/sessions`
- `GET /v1/checkout/sessions/{checkoutSessionId}`
- `GET /v1/payments/{paymentId}`
- `POST /v1/pilot/manual-acceptances/{paymentId}`
- `GET /v1/pilot/manual-acceptances/{paymentId}`
- `PUT /v1/webhook-endpoint`
- public Hosted Checkout evidence contract using `VEROX-Checkout-Capability`

Payment `status` and `effective_status` are distinct. `MANUALLY_ACCEPTED` is an effective state and must never be represented as automatic `CONFIRMED` verification.

The frontend does **not** fabricate production-facing implementations for APIs that are not currently exposed, including payment collection/search, review queues, team/human-user/RBAC, payment-channel management, webhook delivery history, API logs or tenant administration.

Tenant readiness is preserved through reusable merchant/account/domain types and merchant-scoped service boundaries without implementing speculative multi-tenant administration.
