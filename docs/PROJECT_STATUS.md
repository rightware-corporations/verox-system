# VEROX MVP — Project Status

## Objective

Deliver an operational VEROX MVP in four days for the first real integration.

## Current phase

**PHASE 1 — Core Platform**

Status: **IN PROGRESS**

## Completed

- Product boundary defined
- Hosted checkout ownership defined
- Payment source-of-truth rule defined
- MVP repository structure defined
- Backend technology selected
- Initial PostgreSQL baseline migration defined
- Health/Actuator foundation defined
- Merchant domain model implemented
- API key generation and SHA-256 storage implemented
- Stateless Bearer API key authentication implemented
- `GET /v1/account` authentication probe implemented
- One-time MVP merchant bootstrap implemented
- Authentication unit tests added

## Current task

`VX-CORE-003 — CheckoutSession + Payment creation`

Definition of Done:

- authenticated merchant can create a Checkout Session
- Checkout Session and Payment are created atomically
- merchant `external_reference` is preserved
- amount is stored in minor units
- idempotency key prevents duplicate creation
- a non-enumerable hosted checkout URL is returned
- merchant can retrieve its own Checkout Session and Payment
- another merchant cannot retrieve them

## Validation pending

`VX-CORE-001` and `VX-CORE-002` are implemented in `main`, but CI/runtime validation is still required before marking them fully verified.

## Next tasks

1. `VX-CORE-003 — CheckoutSession + Payment creation`
2. Hosted Checkout frontend bootstrap
3. Customer evidence upload
4. Provider evidence bridge

## Scope rule

No feature enters the four-day MVP unless it directly supports creation, verification, confirmation, or return of a real payment.
