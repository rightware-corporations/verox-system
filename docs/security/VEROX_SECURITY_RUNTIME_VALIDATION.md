# VEROX Security Runtime Validation

## Current gate

**SECURITY CLOSURE AUDIT — WAVE A**

Status: **IN PROGRESS**

Current task: `SEC-RT-001 — prove stale Provider Evidence cannot confirm a Checkout created after the evidence arrived`

Next task: `SEC-RT-002 — prove TEST/LIVE evidence isolation at runtime`

## Validated baseline — 2026-08-12

### SECURITY-WAVE-A-01 / SEC-049

Status: **VERIFIED**

Local regression suite:

- tests run: 49
- failures: 0
- errors: 0
- skipped: 0
- Maven result: BUILD SUCCESS

The Verification Engine now evaluates real Customer/Provider evidence pairs; unrelated or malformed Customer Evidence cannot by itself poison a legitimate Payment into permanent review.

### SECURITY-WAVE-A-02 — implementation regression suite

Status: **IMPLEMENTED / UNIT-REGRESSION VALIDATED / RUNTIME ATTACK TESTS PENDING**

Local regression suite after environment and temporal-eligibility changes:

- tests run: 51
- failures: 0
- errors: 0
- skipped: 0
- Maven result: BUILD SUCCESS

### Flyway V7 — environment and match eligibility

Status: **LOCAL DATABASE VALIDATED**

Validated against PostgreSQL 18.4:

- `flyway_schema_history` reports version `7` with `success = true`;
- `bridges.environment` exists and is `NOT NULL`;
- `evidences.environment` exists and is `NOT NULL`;
- existing Bridge rows were backfilled to `TEST`;
- existing Customer and Provider Evidence rows were backfilled to `TEST`;
- application started successfully after V7 with Hibernate schema validation enabled.

Observed local counts at validation time:

```text
bridges
TEST = 1

evidences
TEST / CUSTOMER = 9
TEST / PROVIDER = 7
```

## Security invariants under runtime validation

### SEC-032 + SEC-038

Provider Evidence used for automatic matching must be eligible for the target Checkout. Evidence whose server-observed `created_at` predates the Checkout creation time must not automatically confirm that Checkout.

### SEC-034

Bridge and Evidence environment are part of the trust domain. TEST evidence must not satisfy LIVE Payments, and LIVE evidence must not satisfy TEST Payments.

### SEC-052

Bridge-supplied `received_at` is metadata, not the authoritative clock for automatic-match eligibility. Server-observed `created_at` is used for the eligibility window.

## Required runtime tests

### SEC-RT-001 — stale Provider Evidence

Expected result:

```text
Provider Evidence created before Checkout
+
new Checkout with matching-looking Customer Evidence
=
Payment must NOT become CONFIRMED
```

### SEC-RT-002 — environment isolation

Expected result:

```text
TEST Payment + LIVE Provider Evidence = NOT CONFIRMED
LIVE Payment + TEST Provider Evidence = NOT CONFIRMED
```

Only after these controlled runtime tests pass may SEC-032, SEC-034, SEC-038 and SEC-052 be marked VERIFIED.
