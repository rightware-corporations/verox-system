# VEROX MVP — Project Status

## Objective

Deliver an operational VEROX MVP in four days for the first real integration.

## Current phase

**PHASE 2 — Hosted Checkout**

Status: **IN PROGRESS**

Current task:

`VX-CHECKOUT-01 — Bootstrap frontend/checkout with TypeScript / TSX`

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

## Phase 2 — Hosted Checkout

Planned tasks:

1. bootstrap `frontend/checkout` with TypeScript / TSX
2. expose public Checkout Session read model
3. render payment summary and M-Pesa instructions
4. implement customer evidence upload
5. implement verifying/success/failure checkout states

## Scope rule

No feature enters the four-day MVP unless it directly supports creation, verification, confirmation, or return of a real payment.
