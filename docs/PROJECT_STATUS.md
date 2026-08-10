# VEROX MVP — Project Status

## Objective

Deliver an operational VEROX MVP in four days for the first real integration.

## Current phase

**PHASE 4 — VEROX Webhook Delivery**

Status: **IN PROGRESS — VX-WEBHOOK-01 RUNTIME VALIDATED; VX-WEBHOOK-02 HTTP DELIVERY + RETRY IMPLEMENTED, LOCAL VALIDATION PENDING**

Current task:

`VX-WEBHOOK-02-VALIDATE — Validate signed HTTP POST delivery, success persistence and bounded retry/backoff`

Next task after validation:

`BACKEND ACCEPTANCE CLOSEOUT — reverse Provider-before-Customer E2E + remaining evidence/credential replay checks, then PHASE 5 hardening/Railway`

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
- at least one cross-credential misuse path returned HTTP 401; opposite-direction isolation check remains a backend acceptance closeout item

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
- duplicate identical customer-message replay/database-count validation remains a backend acceptance closeout item

## Phase 3 — VEROX Verification Engine

Status: **IMPLEMENTATION DONE — CUSTOMER-FIRST LOCAL RUNTIME E2E VALIDATED; REVERSE ARRIVAL-ORDER ACCEPTANCE REMAINS BEFORE BACKEND DOD**

Implemented and validated:

- M-Pesa CUSTOMER / PROVIDER parser
- normalized transaction reference and MZN minor-unit amount
- conservative deterministic matcher
- Payment expected amount/currency validation
- ambiguity and malformed inputs never guess confirmation
- provider reference replay protection in application and PostgreSQL (Flyway V5)
- Verification Orchestrator separated from Evidence ingestion
- neutral Evidence ingestion events
- `AFTER_COMMIT` verification with `REQUIRES_NEW`
- CUSTOMER Evidence automatically moves Payment to `VERIFYING` while waiting for Provider Evidence
- matching PROVIDER Evidence from VEROX Bridge automatically moves Payment to `CONFIRMED`
- both CUSTOMER and PROVIDER Evidence link to the same Payment
- `provider`, `provider_transaction_reference` and `confirmed_at` persist on Payment
- owning Checkout Session transitions `OPEN → COMPLETED`
- Checkout `completed_at` is populated from confirmation time
- runtime E2E returned `Payment.status = CONFIRMED`, `Payment.provider = MPESA`, `Checkout.status = COMPLETED`, `Checkout.payment_status = CONFIRMED`
- full local suite passed with 40 tests, 0 failures and 0 errors before Phase 4

Acceptance closeout still required before backend Definition of Done:

- run runtime E2E with PROVIDER Evidence arriving before CUSTOMER Evidence and confirm the same deterministic result

### Verification safety rule — LOCKED

False positive is worse than false negative.

VEROX must never guess a payment match. Missing, malformed, conflicting or ambiguous evidence goes to `REVIEW_REQUIRED` or remains pending; it does not become `CONFIRMED`.

OCR output is never sufficient by itself to confirm a payment.

## Phase 4 — VEROX Webhook Delivery

### VX-WEBHOOK-01 — DONE / LOCAL RUNTIME OUTBOX VALIDATED

Implemented and validated:

- Flyway V6 creates `webhook_endpoints`, `webhook_events` and `webhook_deliveries`
- V6 applied successfully against local PostgreSQL 18.4
- one configured webhook endpoint per Merchant for the MVP
- `PUT /v1/webhook-endpoint` merchant-authenticated configuration endpoint
- endpoint IDs use non-enumerable `whep_*`
- merchant signing secrets use `whsec_*`
- `whsec_*` is deterministically derived from a server-side `VEROX_WEBHOOK_MASTER_SECRET` + endpoint identity instead of being stored in plaintext in PostgreSQL
- HMAC-SHA256 signature format: `VEROX-Signature: t=<unix>,v1=<hex>` over `<timestamp>.<raw-payload>`
- configured local endpoint returned `whep_*`, `ACTIVE` and a valid `whsec_*` secret shape
- Verification Engine publishes neutral `PaymentConfirmedEvent` only after deterministic Payment confirmation
- Webhook Delivery listens `AFTER_COMMIT`; webhook failures cannot roll back confirmed Payments
- `payment.confirmed` webhook event payload is serialized once and persisted as immutable `payload_json`
- webhook event IDs use `evt_*`
- duplicate event protection exists per Merchant + event type + aggregate
- active Merchant endpoint creates exactly one persistent delivery row per event
- delivery IDs use `wd_*`
- new delivery starts `PENDING` with `attempt_count = 0`
- runtime confirmed one `evt_* / payment.confirmed / PAYMENT` row for a newly confirmed Payment
- runtime confirmed one `wd_* / PENDING / attempt_count=0` row linked to the active `whep_*`

### VX-WEBHOOK-02 — HTTP DELIVERY + RETRY IMPLEMENTED / LOCAL VALIDATION PENDING

Implemented:

- scheduled persistent delivery worker reads due `PENDING` / `FAILED` deliveries
- raw persisted webhook payload is POSTed as `application/json`
- Java HTTP transport uses bounded connect/request timeouts and does not follow redirects
- request carries `VEROX-Event-Id`, `VEROX-Event-Type`, `VEROX-Delivery-Id` and `VEROX-Signature`
- signature is generated from the exact persisted raw payload immediately before each attempt
- any HTTP 2xx marks the delivery `SUCCEEDED`, stores status code, attempt time and `delivered_at`
- non-2xx and transport errors persist `FAILED`, increment `attempt_count` and schedule bounded exponential retry
- retries use configurable base/max delay and configurable maximum attempts
- after maximum attempts the delivery becomes `EXHAUSTED` and leaves the retry index
- Payment and Checkout state are never changed by webhook delivery outcomes
- local merchant receiver script added at `infrastructure/dev/webhook-receiver.ps1`
- development receiver validates the real HMAC signature before returning HTTP 200
- delivery state-machine and signed-delivery unit tests added
- no new Flyway migration is required because V6 already contains all attempt/result fields

Validation gate:

1. pull latest `main`
2. run full local test suite
3. restart VEROX Server
4. with no receiver listening, confirm an existing/new due delivery becomes `FAILED`, increments `attempt_count` and receives a future `next_attempt_at`
5. start `infrastructure/dev/webhook-receiver.ps1` with the merchant `whsec_*` supplied only through a local environment variable
6. confirm the receiver accepts a retry only when `VEROX-Signature` is valid
7. confirm the same `wd_*` becomes `SUCCEEDED`, `last_status_code = 200` and `delivered_at` is populated
8. confirm no second webhook event/delivery row is created for the same Payment event
9. run one fresh confirmed-payment E2E with receiver already online and confirm direct first-attempt success

## Phase 5 — Backend Hardening / Railway Deployment

Planned:

- remove/default-disable Spring Security development credential behavior
- require production webhook master secret with no development fallback
- production secrets
- rate limiting
- audit/security logging
- evidence retention/access rules
- SSRF/network-egress policy for production webhook endpoints while preserving local development support
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
11. VEROX persists and HMAC-signs merchant webhook events
12. failed webhook delivery retries safely
13. flow runs on Railway with PostgreSQL
14. OCR alone can never confirm a payment
15. no manual database intervention is required

## Scope rule

No feature enters the four-day MVP unless it directly supports creation, verification, confirmation, or return of a real payment.
