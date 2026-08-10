# VEROX MVP — Project Status

## Objective

Deliver an operational VEROX MVP in four days for the first real integration.

## Current phase

**PHASE 3 — VEROX Verification Engine**

Status: **IN PROGRESS — AUTOMATIC CUSTOMER ↔ PROVIDER VERIFICATION RUNTIME VALIDATED; CHECKOUT COMPLETION IMPLEMENTED, LOCAL VALIDATION PENDING**

Current task:

`VX-VERIFY-05-VALIDATE — Validate Checkout Session OPEN → COMPLETED when its Payment becomes CONFIRMED`

Next task after validation:

`PHASE 4 / VX-WEBHOOK-01 — Implement merchant webhook configuration, payment.confirmed event persistence and HMAC signing`

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

Implemented and validated:

- Java 21 / Spring Boot backend foundation
- PostgreSQL + Flyway
- Merchant domain and API keys
- stateless Bearer merchant authentication
- one-time merchant bootstrap
- Checkout Session + Payment persistence
- `POST /v1/checkout/sessions`
- `GET /v1/checkout/sessions/{id}`
- `GET /v1/payments/{id}`
- `GET /v1/account`
- merchant-scoped resources
- MZN minor-unit normalization
- idempotency and request fingerprinting
- non-enumerable `cs_*` and `pay_*` IDs
- Hosted Checkout URL generation
- API error envelope
- Maven Wrapper and local PostgreSQL runtime validation

## Phase 2 — Evidence Infrastructure / VEROX Bridge

### VX-EVIDENCE-01 — DONE / LOCAL RUNTIME VALIDATED

Implemented and validated:

- Flyway V3 `evidences` schema
- non-enumerable `ev_*` IDs
- `CUSTOMER` / `PROVIDER` evidence origins
- `IMAGE`, `SMS`, `PDF`, `TEXT`, `JSON` evidence kinds
- `HOSTED_CHECKOUT`, `VEROX_BRIDGE`, `PROVIDER_API`, `INTERNAL` ingest sources
- SHA-256 evidence hashing
- immutable raw evidence semantics
- provider evidence can arrive unlinked before matching
- customer evidence can be linked directly to Payment
- evidence deduplication support

### VX-BRIDGE-01 — FUNCTIONAL RUNTIME VALIDATED

Implemented and validated:

- Flyway V4 `bridges` + `bridge_credentials`
- non-enumerable `brg_*` IDs
- dedicated `vx_bridge_*` credential namespace
- SHA-256 bridge credential storage
- `ROLE_BRIDGE` separated from `ROLE_MERCHANT`
- Bridge scoped to Merchant + provider
- one-time Bridge bootstrap
- `POST /v1/bridges/{bridgeId}/evidence`
- raw SMS persists as `Evidence(PROVIDER, SMS, VEROX_BRIDGE)`
- provider Evidence remains `payment_id = NULL` until Verification Engine matching
- identical provider SMS replay returns the same `ev_*`
- database count remained one after replay
- at least one cross-credential misuse path returned HTTP 401; opposite-direction isolation check remains a small hardening closeout item

### VX-EVIDENCE-02 — CUSTOMER MESSAGE FLOW FUNCTIONAL RUNTIME VALIDATED

Implemented and runtime validated:

- customer pastes one complete payment confirmation message
- no manual transaction fields
- `POST /public/v1/checkout/{checkoutSessionId}/evidence/message`
- request body contains only `content`
- server assigns receipt timestamp
- Checkout Session must be `OPEN` and unexpired
- customer message persists as `Evidence(CUSTOMER, SMS, HOSTED_CHECKOUT)`
- customer Evidence is linked directly to the Checkout Session Payment
- provider recorded as `MPESA` for the MVP
- endpoint response returned `ev_*`, `cs_*`, `pay_*`, `CUSTOMER`, `SMS`, `HOSTED_CHECKOUT`, `MPESA`
- duplicate identical customer-message replay/database-count validation remains a closeout hardening check

## Phase 3 — VEROX Verification Engine

### VX-VERIFY-01 — M-PESA MESSAGE PARSER DONE / LOCAL VALIDATED

Implemented and validated:

- normalized `ParsedMpesaMessage` model
- parser keeps CUSTOMER and PROVIDER message rules separate
- CUSTOMER sample format: `Confirmado <reference>. Transferiste <amount>MT ...`
- PROVIDER sample format: `<reference> Confirmed.You have received <amount>MT ...`
- transaction reference normalized to uppercase
- amount normalized to MZN minor units
- parser only marks a message match-ready when recognized reference + amount are present
- unknown/unrecognized text does not become match-ready
- current parser intentionally supports only validated MVP patterns; broader provider-format support requires real samples
- parser never changes Payment status and never confirms a payment
- full local suite passed with 22 tests, 0 failures and 0 errors after parser implementation

### VX-VERIFY-02 — CONSERVATIVE MATCHER DONE / LOCAL VALIDATED

Implemented and validated:

- pure `MpesaEvidenceMatcher` separated from Payment mutation/orchestration
- exact CUSTOMER ↔ PROVIDER transaction-reference match required
- exact CUSTOMER ↔ PROVIDER amount/currency match required
- provider amount/currency must also match expected Payment amount/currency
- malformed, conflicting or mismatched input returns `REVIEW_REQUIRED`
- matcher does not link Evidence, mutate Payment or emit webhooks
- full local suite passed with 29 tests, 0 failures and 0 errors after matcher implementation

### VX-VERIFY-03 — VERIFICATION ORCHESTRATOR DONE / LOCAL VALIDATED

Implemented and validated:

- internal `VerificationOrchestrator` separated from Evidence ingestion endpoints
- reads CUSTOMER Evidence already linked to a Payment
- rejects conflicting distinct customer-message claims as `REVIEW_REQUIRED`
- validates customer amount/currency against expected Payment before provider matching
- scans only unlinked `PROVIDER/SMS/VEROX_BRIDGE/MPESA` Evidence for the Merchant
- unrelated provider SMS with another transaction reference is ignored; Payment keeps waiting
- same-reference provider candidate is passed through the conservative matcher
- multiple provider candidates with the same reference become `REVIEW_REQUIRED`
- exactly one deterministic match links the provider Evidence to the Payment
- controlled Payment transitions: `PENDING → VERIFYING → CONFIRMED` or `REVIEW_REQUIRED`
- provider + provider transaction reference are persisted only after deterministic match
- application-level provider-reference reuse check prevents known reuse across Payments
- Flyway V5 adds a database unique index on `(merchant_id, provider, provider_transaction_reference)` as a final replay/concurrency safety barrier
- full local suite passed with 35 tests, 0 failures and 0 errors after orchestrator implementation

### VX-VERIFY-04 — DONE / RUNTIME E2E VALIDATED

Implemented and validated:

- Evidence Infrastructure publishes neutral `EvidenceIngestedEvent` after CUSTOMER/PROVIDER ingestion
- Verification Engine owns the `AFTER_COMMIT` listener; VEROX Bridge remains independent from matcher internals
- post-commit verification runs in a new transaction (`REQUIRES_NEW`)
- CUSTOMER evidence automatically moved the Payment from `PENDING` to `VERIFYING`
- matching PROVIDER evidence through VEROX Bridge automatically moved the same Payment to `CONFIRMED`
- runtime Payment persisted `provider = MPESA`, unique `provider_transaction_reference` and `confirmed_at`
- CUSTOMER and PROVIDER Evidence were both linked to the same Payment
- PROVIDER Evidence persisted `linked_at`
- Flyway V5 was applied successfully
- full local suite passed with 38 tests, 0 failures and 0 errors before runtime E2E

### VX-VERIFY-05 — CHECKOUT COMPLETION IMPLEMENTED / LOCAL VALIDATION PENDING

Implemented:

- Checkout Session domain transition `OPEN → COMPLETED`
- `completed_at` uses the Payment confirmation timestamp
- repeated completion is idempotent and does not overwrite the first completion timestamp
- non-OPEN/non-COMPLETED Checkout states cannot be completed
- deterministic Payment confirmation completes its owning Checkout Session in the same verification transaction
- no new migration required because `checkout_sessions.completed_at` and `COMPLETED` already exist
- unit tests cover completion and idempotency

Validation gate:

1. pull latest `main`
2. run full local test suite
3. run a fresh runtime E2E pair
4. confirm Payment becomes `CONFIRMED`
5. confirm owning Checkout Session becomes `COMPLETED`
6. confirm `completed_at` is populated

### Verification safety rule — LOCKED

False positive is worse than false negative.

VEROX must never guess a payment match. Missing, malformed, conflicting or ambiguous evidence goes to `REVIEW_REQUIRED` or remains pending; it does not become `CONFIRMED`.

OCR output is never sufficient by itself to confirm a payment.

## Phase 4 — Webhooks / Merchant Return

Planned:

- merchant webhook configuration
- HMAC signing
- `payment.confirmed` and related event contracts
- persistent delivery attempts
- retry/backoff
- webhook idempotency
- merchant integration validation

## Phase 5 — Backend Hardening / Railway Deployment

Planned:

- remove/default-disable Spring Security development credential behavior
- production secrets
- rate limiting
- audit/security logging
- evidence retention/access rules
- Railway PostgreSQL
- optional durable object storage for supplementary uploads
- Railway `PORT` + healthcheck
- staging deployment
- production-like end-to-end validation

## Phase 6 — Hosted Checkout Frontend

Planned:

- TypeScript/TSX checkout
- payment summary + M-Pesa instructions
- one-field pasted-message flow
- optional supplementary image upload
- verifying/success/failure states
- success/cancel redirect UX
- deployment

## MVP backend Definition of Done

The backend is not complete until:

1. merchant creates a Checkout Session
2. customer pastes the complete confirmation message and VEROX persists Customer Evidence
3. official receiving SMS enters through VEROX Bridge
4. either evidence may arrive first
5. VEROX parses both independently sourced messages
6. VEROX safely matches the correct evidence pair
7. ambiguity never becomes a guessed confirmation
8. matched evidence transitions Payment to `CONFIRMED`
9. owning Checkout Session transitions to `COMPLETED`
10. duplicate/replayed evidence cannot confirm multiple Payments
11. VEROX emits HMAC-signed merchant webhook events
12. failed webhook delivery retries safely
13. flow runs on Railway with PostgreSQL
14. OCR alone can never confirm a payment
15. no manual database intervention is required

## Scope rule

No feature enters the four-day MVP unless it directly supports creation, verification, confirmation, or return of a real payment.
