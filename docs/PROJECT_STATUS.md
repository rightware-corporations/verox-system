# VEROX MVP — Project Status

## Objective

Deliver an operational VEROX MVP in four days for the first real integration.

## Current phase

**PHASE 2 — Backend Completion / Evidence Infrastructure**

Status: **IN PROGRESS — BRIDGE FUNCTIONAL RUNTIME VALIDATED; CUSTOMER MESSAGE INGESTION IMPLEMENTED, LOCAL VALIDATION PENDING**

Current task:

`VX-EVIDENCE-02-MESSAGE-VALIDATE — Validate pasted customer M-Pesa confirmation message ingestion locally`

Next task after validation:

`VX-VERIFY-01 — Implement M-Pesa message parsing for CUSTOMER and PROVIDER evidence`

## Delivery strategy

Backend-first for the MVP. The Hosted Checkout frontend will be implemented only after the backend payment-verification flow is complete and deployable.

The local Windows environment is used only for development and validation. Production/staging deployment target is Railway.

Runtime component names and boundaries are locked in `docs/architecture/runtime-components.md`: VEROX Server, VEROX Merchant API, VEROX Hosted Checkout, VEROX Bridge, VEROX Evidence Infrastructure, VEROX Verification Engine and VEROX Webhook Delivery are separate responsibilities even while the MVP is deployed as one modular-monolith backend process.

### Customer evidence rule — LOCKED

For the MVP, the customer does not manually fill transaction reference, amount, phone number or other payment fields.

The primary customer action is to copy and paste the complete M-Pesa payment confirmation message into one field in VEROX Hosted Checkout.

That full message is stored as immutable `Evidence(CUSTOMER, SMS, HOSTED_CHECKOUT)` and is parsed later by the VEROX Verification Engine.

An uploaded screenshot/image may be supported as supplementary evidence, but OCR is extraction assistance only and is not required to confirm a payment. No payment may be confirmed directly from OCR output.

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

### VX-EVIDENCE-01 — DONE / LOCAL RUNTIME VALIDATED

Implemented and validated:

- VEROX runtime component/layer naming boundaries documented
- Flyway V3 `evidences` schema
- non-enumerable `ev_*` Evidence IDs
- evidence origin model: `CUSTOMER`, `PROVIDER`
- evidence kind model: `IMAGE`, `SMS`, `PDF`, `TEXT`, `JSON`
- ingestion source model: `HOSTED_CHECKOUT`, `VEROX_BRIDGE`, `PROVIDER_API`, `INTERNAL`
- SHA-256 evidence content hashing
- customer evidence can be linked directly to a Payment
- provider evidence can be persisted unlinked before matching, so provider SMS may arrive before customer proof
- unlinked provider evidence query support
- duplicate provider evidence protection by merchant/content hash
- duplicate customer evidence protection by payment/content hash
- Evidence service separates provider/Bridge ingestion from customer/Hosted Checkout ingestion
- Verification Engine can later link provider evidence to a Payment without changing evidence content
- 11 local tests passed with 0 failures and 0 errors before Bridge implementation
- Flyway V3 applied successfully in local PostgreSQL
- `evidences` table exists in local PostgreSQL
- `flyway_schema_history` records version `3` / `evidence infrastructure` with `success = true`

### VX-BRIDGE-01 — FUNCTIONAL RUNTIME VALIDATED / FINAL ISOLATION CHECK PENDING

Implemented and runtime validated:

- Flyway V4 `bridges` and `bridge_credentials` schema
- non-enumerable `brg_*` Bridge IDs
- dedicated `vx_bridge_*` credential namespace
- bridge credentials are SHA-256 hashed at rest
- Bridge status and credential status models
- Bridge is scoped to a Merchant and provider
- dedicated VEROX Bridge authentication principal and filter
- Merchant API key filter explicitly excludes Bridge endpoints
- security roles separate `ROLE_BRIDGE` from `ROLE_MERCHANT`
- bridge credential cannot be used against another `bridgeId`
- one-time Bridge bootstrap provisioning for MVP/local setup
- `POST /v1/bridges/{bridgeId}/evidence`
- raw Bridge payload is persisted as `Evidence(PROVIDER, SMS, VEROX_BRIDGE)`
- provider evidence remains unlinked to Payment until Verification Engine matching
- duplicate provider SMS returns the existing Evidence instead of creating a second immutable evidence record
- Bridge provisioning/authentication and ingestion/scoping unit tests added
- complete local test suite passed after Bridge implementation
- Flyway V4 applied successfully in local PostgreSQL
- local `brg_*` Bridge and `vx_bridge_*` credential provisioned
- raw provider SMS posted successfully through Bridge endpoint
- resulting provider Evidence persisted with `payment_id = NULL`
- replaying the identical SMS returned the same `ev_*`
- database count remained exactly one row after replay
- at least one cross-credential misuse path returned HTTP `401`; the opposite direction remains a final isolation check

### VX-EVIDENCE-02 — CUSTOMER MESSAGE INGESTION IMPLEMENTED / LOCAL VALIDATION PENDING

Implemented:

- customer provides one complete pasted payment confirmation message, with no manual transaction fields
- `POST /public/v1/checkout/{checkoutSessionId}/evidence/message`
- request body contains only `content`
- server assigns the receipt timestamp; customer does not submit authoritative timing metadata
- checkout session must exist, be `OPEN` and not be expired
- customer message persists as `Evidence(CUSTOMER, SMS, HOSTED_CHECKOUT)`
- customer Evidence is linked directly to the Checkout Session Payment
- MVP provider is recorded as `MPESA`
- content is SHA-256 hashed and duplicate identical customer messages for the same Payment reuse the existing Evidence
- endpoint returns evidence/payment/session metadata without echoing the raw message
- customer-message ingestion unit tests added

Validation gate:

1. pull latest `main` and run full local test suite
2. restart VEROX Server with both bootstraps disabled
3. submit a pasted M-Pesa customer confirmation message against an active `cs_*`
4. confirm response returns one `ev_*` with `origin=CUSTOMER`, `kind=SMS`, `ingest_source=HOSTED_CHECKOUT`
5. confirm the Evidence row is linked to the expected `payment_id`
6. replay the identical customer message and confirm no duplicate Evidence row is created

### Remaining Phase 2 tasks

1. finish the opposite Bridge/Merchant credential-isolation runtime check
2. public Checkout Session read model required by the later Hosted Checkout frontend
3. optional durable object-storage abstraction for supplementary uploaded images
4. optional customer image-upload endpoint with file type/size validation
5. evidence audit/replay hardening under concurrent ingestion

## Phase 3 — Verification Engine

Planned tasks:

1. M-Pesa CUSTOMER confirmation-message parser
2. M-Pesa PROVIDER receiving-message parser
3. normalized evidence fields shared across both sources
4. matching engine using transaction reference, amount, provider, timestamp and identity signals
5. payment state machine: `PENDING`, `VERIFYING`, `CONFIRMED`, `REVIEW_REQUIRED`, `FAILED`, `EXPIRED`
6. false-positive-safe matching thresholds
7. transaction reference reuse protection
8. verification tests using paired M-Pesa samples
9. optional OCR adapter for supplementary customer image evidence; OCR output never confirms a payment by itself

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
7. durable evidence object storage configuration when supplementary uploads are enabled
8. Railway `PORT`/healthcheck configuration
9. deploy backend to Railway staging
10. run production-like end-to-end API/verification tests

## Phase 6 — Hosted Checkout Frontend

Planned tasks:

1. bootstrap `frontend/checkout` with TypeScript / TSX
2. render payment summary and M-Pesa instructions
3. one-field customer flow to paste the complete M-Pesa confirmation message
4. optional supplementary evidence upload UX
5. verifying/success/failure states
6. success/cancel redirect UX
7. deploy Hosted Checkout

## MVP backend Definition of Done

The backend is not considered complete until:

1. merchant creates a Checkout Session
2. customer can paste the complete payment confirmation message and VEROX persists it as Customer Evidence
3. official M-Pesa receiving SMS can be ingested through the VEROX Bridge endpoint
4. provider evidence can safely arrive before or after customer evidence
5. VEROX parses both customer and provider messages
6. VEROX compares and evaluates the two independently sourced messages
7. ambiguous evidence becomes `REVIEW_REQUIRED`, never guessed as confirmed
8. matched evidence transitions Payment to `CONFIRMED`
9. duplicate/replayed evidence cannot confirm multiple payments
10. VEROX emits an HMAC-signed merchant webhook
11. failed webhook deliveries are retried safely
12. the flow runs on Railway with PostgreSQL
13. no OCR result is sufficient by itself to confirm a payment
14. no manual database intervention is required

## Scope rule

No feature enters the four-day MVP unless it directly supports creation, verification, confirmation, or return of a real payment.
