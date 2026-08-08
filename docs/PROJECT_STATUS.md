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

## Current task

`VX-CORE-001 — Bootstrap backend`

Definition of Done:

- Spring Boot application exists
- PostgreSQL configuration exists
- Flyway baseline exists
- Health endpoint is exposed
- Local database can be started from Docker Compose
- Application can start against the local database

## Next tasks

1. `VX-CORE-002 — Merchant + API Key authentication`
2. `VX-CORE-003 — CheckoutSession + Payment creation`
3. Hosted Checkout frontend bootstrap

## Scope rule

No feature enters the four-day MVP unless it directly supports creation, verification, confirmation, or return of a real payment.
