# VEROX Runtime Components and Layer Boundaries

## Naming rule

VEROX components are named by responsibility. Names must not be used interchangeably in code, documentation, logs, endpoints, or deployment configuration.

## VEROX Server

The deployable Java/Spring Boot backend. In production/staging this runs on Railway.

The VEROX Server owns:

- Merchant API authentication and checkout resources
- payment source of truth
- Evidence Infrastructure
- Verification Engine orchestration
- webhook delivery
- persistence and audit state

The VEROX Server does not read a phone inbox directly and does not depend on the Hosted Checkout frontend to confirm a payment.

## VEROX Merchant API

The authenticated API surface used by merchant systems.

Examples:

- `POST /v1/checkout/sessions`
- `GET /v1/checkout/sessions/{id}`
- `GET /v1/payments/{id}`
- `GET /v1/account`

The Merchant API can create and read payment resources, but no merchant endpoint is allowed to force `Payment=CONFIRMED`.

## VEROX Hosted Checkout

The browser-facing VEROX payment UX. It is a client of public VEROX Server endpoints and will be implemented after the backend verification flow is complete.

Responsibilities:

- display payment summary
- display M-Pesa instructions
- accept customer proof/evidence upload
- display verifying/success/failure state
- redirect for UX only

A browser redirect is never authoritative payment confirmation.

## VEROX Bridge

The trusted transport between a receiving device/channel and the VEROX Server.

For the first MVP the transport is:

`M-Pesa official SMS -> iPhone Personal Automation / Shortcut -> VEROX Bridge endpoint -> VEROX Server`

The phone/Shortcut transports raw provider evidence. It must not implement payment matching or confirmation logic.

Bridge API target:

`POST /v1/bridges/{bridgeId}/evidence`

Bridge authentication uses a dedicated bridge credential, separate from merchant API keys.

Future bridge sources may include a VEROX mobile app or another trusted capture agent without changing the Verification Engine contract.

## VEROX Evidence Infrastructure

The persistence and ingestion layer for immutable payment evidence.

It distinguishes:

- evidence origin: `CUSTOMER` or `PROVIDER`
- evidence kind: `IMAGE`, `SMS`, `PDF`, `TEXT`, `JSON`
- ingestion source: `HOSTED_CHECKOUT`, `VEROX_BRIDGE`, `PROVIDER_API`, `INTERNAL`

Evidence stores identity, hashes, timestamps, provider, storage references/raw content and audit metadata. Evidence existence is independent from Payment state because customer proof and provider evidence can arrive in either order.

## VEROX Verification Engine

The internal payment-decision engine. It is not a public API and it is not implemented in the Bridge or frontend.

Pipeline:

`Evidence -> Extraction/Parser -> Normalization -> Matching -> Decision -> Payment state transition`

MVP components:

- M-Pesa provider SMS parser
- OCR adapter for customer proof
- normalized evidence fields
- matching rules using provider transaction reference, amount, provider, timestamp and identity signals
- ambiguity handling with `REVIEW_REQUIRED`
- duplicate/replay protection

Safety rule: prefer a false negative/review over a false positive. Ambiguous evidence must never be guessed as `CONFIRMED`.

## VEROX Webhook Delivery

The outbound event layer from VEROX Server to merchant systems.

Responsibilities:

- persistent event records
- HMAC signatures
- retry/backoff
- idempotent delivery
- events such as `payment.confirmed`

The signed webhook is the merchant integration source of truth for asynchronous payment confirmation. Redirects are UX only.

## Runtime flow

```text
Merchant System
    |
    | Merchant API
    v
VEROX Server
    |
    +--> CheckoutSession + Payment
    |
    +<-- Hosted Checkout customer evidence
    |
M-Pesa receiving phone
    |
    | VEROX Bridge (raw official SMS)
    v
VEROX Evidence Infrastructure
    |
    v
VEROX Verification Engine
    |
    +--> CONFIRMED / REVIEW_REQUIRED / FAILED
    |
    v
VEROX Webhook Delivery
    |
    v
Merchant System
```

## Deployment boundary

For the MVP there is one modular-monolith VEROX Server process, not microservices. Bridge, Evidence, Verification and Webhook are separate modules/responsibilities inside the architecture even when deployed in the same backend process.

The local Windows machine is development/test only. Production/staging backend runs on Railway with PostgreSQL and durable object storage for uploaded evidence.
