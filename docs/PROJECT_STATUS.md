# VEROX MVP — Project Status

## Objective

Deliver an operational VEROX MVP in four days for the first real integration.

## Current phase

**PHASE 2 — Backend Completion / Evidence Infrastructure**

Status: **IN PROGRESS**

Current task:

`VX-EVIDENCE-01 — Define and implement customer/provider evidence persistence model`

## Delivery strategy

Backend-first for the MVP. The Hosted Checkout frontend will be implemented only after the backend payment-verification flow is complete and deployable.

The local Windows environment is used only for development and validation. Production/staging deployment target is Railway.

## Phase 1 — Core Platform

Status: **DONE — LOCAL RUNTIME VALIDATED**

### Completed in implementation

- Product boundary defined
- Hosted checkout ownership defined
- Payment source-of-truth rule defined
- MVP repository structure defined
- Java 21 / Spring Boot backend foundation
- PostgreSQL + Flyway baseline
- Health/Actuator foundation
- Merchant domain model
- API key generation and SHA-256 storage
- Stateless Bearer API key authentication
- `GET /v1/account`
- One-time MVP merchant bootstrap
- Checkout Session schema and domain model
- Payment schema and domain model
- `POST /v1/checkout/sessions`
- `GET /v1/checkout/sessions/{id}`
- `GET /v1/payments/{id}`
- Merchant-scoped resource access
- Amount normalization to MZN minor units
- Idempotency-Key request fingerprinting
- Non-enumerable `cs_*` and `pay_*` public identifiers
- Hosted checkout URL generation
- API error envelope
- Core authentication and checkout unit tests
- Maven Wrapper for Windows/local development
- Spring Boot Flyway starter integration

### Validated locally

- Java runtime available
- Maven Wrapper 3.9.16 works
- backend compiles successfully with `release 21`
- 6 unit tests pass, 0 failures, 0 errors
- PostgreSQL 18.4 installed and running
- local `verox` role/database connection works
- Flyway V1 and V2 migrations apply successfully
- backend starts successfully with PostgreSQL
- `/actuator/health` returns `UP`
- bootstrap provisions a merchant and `vx_test_*` API key
- valid VEROX API key authenticates successfully on `/v1/account`
- invalid/non-VEROX credential is rejected by API-key authentication
- checkout creation returns one `cs_*` Checkout Session and one `pay_*` Payment with `OPEN` / `PENDING`
- repeated request with identical `Idempotency-Key` and payload returns the exact same Checkout Session and Payment IDs
- changed payload with the same `Idempotency-Key` returns HTTP `409 Conflict`

### Validated checkout sample

- external reference: `ORDER-82921`
- amount: `1500.00 MZN`
- checkout status: `OPEN`
- payment status: `PENDING`
- checkout URL generated on local Hosted Checkout base URL

### Security hygiene

- one-time bootstrap must remain disabled after merchant/API-key provisioning
- the generated default Spring Security development password is not a VEROX API credential and must not be used by merchant integrations

## Phase 2 — Backend Completion / Evidence Infrastructure

Planned tasks:

1. evidence persistence model for customer proof and provider SMS
2. public Checkout Session read model for Hosted Checkout
3. customer evidence upload endpoint with file type/size validation
4. storage abstraction for evidence objects
5. bridge credential/domain model
6. `POST /v1/bridges/{bridgeId}/evidence` raw SMS ingestion
7. evidence duplicate/replay protection and audit metadata

## Phase 3 — Verification Engine

Planned tasks:

1. M-Pesa provider SMS parser
2. OCR adapter for customer evidence
3. normalized evidence fields
4. matching engine using transaction reference, amount, provider, timestamp and identity signals
5. payment state machine: `PENDING`, `VERIFYING`, `CONFIRMED`, `REVIEW_REQUIRED`, `FAILED`, `EXPIRED`
6. false-positive-safe matching thresholds
7. transaction reference reuse protection
8. verification tests using paired M-Pesa samples

## Phase 4 — Webhooks / Merchant Return

Planned tasks:

1. merchant webhook configuration
2. HMAC signing
3. `payment.confirmed` and related event contracts
4. delivery persistence
5. retry/backoff
6. webhook idempotency and duplicate handling
7. merchant integration validation

## Phase 5 — Backend Hardening / Railway Deployment

Planned tasks:

1. remove/default-disable Spring Security development credential behavior
2. production secret configuration
3. rate limiting for public/bridge endpoints
4. audit/security logging
5. evidence retention and access-control rules
6. Railway PostgreSQL configuration
7. durable evidence object storage configuration
8. Railway `PORT`/healthcheck configuration
9. deploy backend to Railway staging
10. run production-like end-to-end API/verification tests

## Phase 6 — Hosted Checkout Frontend

Planned tasks:

1. bootstrap `frontend/checkout` with TypeScript / TSX
2. render payment summary and M-Pesa instructions
3. evidence upload UX
4. verifying/success/failure states
5. success/cancel redirect UX
6. deploy Hosted Checkout

## MVP backend Definition of Done

The backend is not considered complete until:

1. merchant creates a Checkout Session
2. customer evidence can be uploaded and persisted
3. official M-Pesa SMS can be ingested through the bridge endpoint
4. VEROX parses both evidence sources
5. VEROX compares and evaluates the evidence
6. ambiguous evidence becomes `REVIEW_REQUIRED`, never guessed as confirmed
7. matched evidence transitions Payment to `CONFIRMED`
8. duplicate/replayed evidence cannot confirm multiple payments
9. VEROX emits an HMAC-signed merchant webhook
10. failed webhook deliveries are retried safely
11. the flow runs on Railway with PostgreSQL and durable evidence storage
12. no manual database intervention is required

## Scope rule

No feature enters the four-day MVP unless it directly supports creation, verification, confirmation, or return of a real payment.
