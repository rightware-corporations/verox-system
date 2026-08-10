# VEROX MVP — Project Status

## Objective

Deliver an operational VEROX MVP in four days for the first real integration.

## Current phase

**PHASE 3 — VEROX Verification Engine**

Status: **IN PROGRESS — M-PESA PARSER VALIDATED; CONSERVATIVE MATCHER IMPLEMENTED, LOCAL VALIDATION PENDING**

Current task:

`VX-VERIFY-02-VALIDATE — Validate deterministic CUSTOMER ↔ PROVIDER matching rules locally`

Next task after validation:

`VX-VERIFY-03 — Implement verification orchestration, provider-evidence linking and controlled Payment transitions`

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

### VX-VERIFY-02 — CONSERVATIVE MATCHER IMPLEMENTED / LOCAL VALIDATION PENDING

Implemented:

- pure `MpesaEvidenceMatcher` separated from Payment mutation/orchestration
- requires CUSTOMER origin on customer input and PROVIDER origin on provider input
- both parsed messages must be match-ready
- exact transaction-reference equality required
- exact CUSTOMER ↔ PROVIDER amount equality required
- exact CUSTOMER ↔ PROVIDER currency equality required
- provider amount must also equal the expected Payment amount
- provider currency must also equal the expected Payment currency
- any malformed, conflicting or mismatched input returns `REVIEW_REQUIRED`
- matcher produces `MATCH` only when reference, amount and currency are all deterministic
- matcher does not link Evidence, mutate Payment or emit webhooks
- unit tests cover successful match, reference mismatch, evidence amount mismatch, expected Payment amount mismatch, unrecognized input, origin mismatch and expected-currency mismatch

Validation gate:

1. pull latest `main`
2. run full local test suite
3. confirm matcher tests pass with zero failures/errors

### VX-VERIFY-03 — NEXT

Planned:

1. read CUSTOMER Evidence linked to a Payment
2. parse CUSTOMER message
3. scan unlinked PROVIDER Evidence for the same Merchant/provider
4. parse PROVIDER candidates
5. invoke conservative matcher against the expected Payment amount/currency
6. reject zero/multiple safe candidates as pending or `REVIEW_REQUIRED`
7. link exactly one provider Evidence to Payment only after a deterministic match
8. controlled Payment transitions `PENDING → VERIFYING → CONFIRMED/REVIEW_REQUIRED`
9. persist provider transaction reference on Payment only after deterministic match
10. prevent one provider transaction/evidence from confirming multiple Payments

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
9. duplicate/replayed evidence cannot confirm multiple Payments
10. VEROX emits HMAC-signed merchant webhook events
11. failed webhook delivery retries safely
12. flow runs on Railway with PostgreSQL
13. OCR alone can never confirm a payment
14. no manual database intervention is required

## Scope rule

No feature enters the four-day MVP unless it directly supports creation, verification, confirmation, or return of a real payment.
