# VEROX MVP — Project Status

## Objective

Deliver an operational VEROX MVP in four days for the first real integration.

## Current phase

**PHASE 1 — Core Platform**

Status: **IMPLEMENTED — RUNTIME/CI VALIDATION PENDING**

## Completed in implementation

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

## Current validation gate

`VX-CORE-VALIDATE — Core Platform runtime validation`

Definition of Done:

- PostgreSQL starts successfully
- Flyway V1 and V2 migrations apply successfully
- backend compiles on Java 21
- unit tests pass
- backend starts with PostgreSQL
- `/actuator/health` returns `UP`
- bootstrap creates one merchant and one API key
- valid API key returns `200` from `/v1/account`
- invalid API key returns `401`
- checkout creation persists one Checkout Session and one Payment atomically
- repeated identical Idempotency-Key returns the existing Checkout Session
- different payload with the same Idempotency-Key returns `409`

## Next phase

**PHASE 2 — Hosted Checkout**

First tasks after the validation gate:

1. bootstrap `frontend/checkout` with TypeScript / TSX
2. expose public Checkout Session read model
3. render payment summary and M-Pesa instructions
4. implement customer evidence upload
5. implement verifying/success/failure checkout states

## Validation note

The repository CI workflow is configured for backend changes, but this environment cannot read push-triggered GitHub Actions runs through the available connector. Local network access to GitHub/Maven is also unavailable here, so runtime/CI verification remains an explicit project gate rather than being assumed complete.

## Scope rule

No feature enters the four-day MVP unless it directly supports creation, verification, confirmation, or return of a real payment.
