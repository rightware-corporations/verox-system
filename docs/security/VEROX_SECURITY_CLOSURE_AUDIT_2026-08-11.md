# VEROX Security Closure Audit — 2026-08-11

## Purpose

This document reconciles the external security materials `VEXOR_SECURITY_MASTER_PLAN_v0.2.1.md` and `VEXOR_SECURITY_AUDIT_2026-08-10.md` against the current VEROX repository before Railway deployment.

It is not a replacement for the Security Master Plan. It is the engineering closure ledger for the current backend snapshot.

Security status vocabulary:

- `CONFIRMED GAP` — current code/configuration directly supports the finding.
- `REPRODUCTION REQUIRED` — static analysis supports the attack path, but runtime reproduction is still required before calling the exploit VERIFIED.
- `TEST REQUIRED` — control exists or a race/error path is plausible, but explicit security behavior has not been exercised.
- `ARCHITECTURAL RISK` — real trust assumption or residual risk that is not equivalent to an immediately exploitable backend bug.
- `GOVERNANCE GAP` — policy/operations requirement not yet defined or enforced.

The security objective remains: **prevent false payment truth and malicious denial of legitimate payment truth.**

---

## 1. Current Security Strengths That Must Be Preserved

The current backend retains important fail-closed properties:

- `Payment.confirm()` is guarded by state and the reviewed code exposes a very small confirmation authority surface.
- Verification does not guess through ambiguity; mismatches do not become `CONFIRMED`.
- Merchant and Bridge authentication use separate credential namespaces and roles.
- Provider transaction reference reuse is backed by a PostgreSQL unique index.
- Customer/provider evidence is persisted before after-commit verification runs.
- Webhook delivery is transactionally decoupled from Payment truth.
- Webhook transport uses bounded timeouts and does not follow redirects.

Security hardening must not weaken these properties.

---

## 2. Reconciled Findings

### SEC-032 — Unbound Provider Evidence Pool / Cross-Payment Claim

**Priority:** CRITICAL  
**Closure status:** `CONFIRMED GAP / REPRODUCTION REQUIRED`

Current `VerificationOrchestrator` loads merchant-wide unlinked PROVIDER evidence and selects by parsed provider transaction reference. `EvidenceRepository` applies no checkout binding and no eligibility time range.

A real provider evidence record therefore remains a bearer-like candidate until it is linked. A caller who obtains the same reference and amount can attempt to present Customer Evidence against another same-merchant checkout.

Static analysis confirms the binding weakness. A controlled runtime reproduction is still required before labeling a concrete exploit VERIFIED.

Required closure direction:

- define server-authoritative eligibility for unlinked Provider Evidence;
- Provider Evidence used for a Payment must not predate that Checkout Session's creation in server-observed time;
- bound the eligible window around the Checkout Session lifecycle;
- combine temporal eligibility with environment isolation, strict pair selection and anti-poisoning behavior;
- retain the single-use provider-reference constraint;
- document residual risk: without provider API integration or a provider-carried checkout identifier, the provider transaction reference remains a bearer-like correlation signal if leaked during the eligible window.

TTL alone is mitigation, not a proof of transaction intent.

---

### SEC-033 — No Rate Limiting

**Priority:** HIGH  
**Closure status:** `CONFIRMED GAP`

The public evidence endpoint is permitted without authentication and no rate-limiting layer is present in the current application security chain or dependency/configuration baseline.

Required closure:

- per-checkout submission limit;
- per-source/IP limit where trustworthy proxy metadata is available;
- Merchant API / Bridge endpoint limits appropriate to credential scope;
- bounded memory/state for an application-layer MVP limiter;
- production edge/WAF rate limiting as an additional control when available.

Rate limiting is defense in depth and does not replace correct verification semantics.

---

### SEC-034 — TEST/LIVE Evidence-Domain Separation Gap

**Priority:** HIGH / production blocker  
**Closure status:** `CONFIRMED GAP`

`Payment` has an `ApiKeyEnvironment`; `Evidence` and `Bridge` currently do not. Provider-evidence selection is merchant-scoped, not environment-scoped.

Required closure:

- add environment to Bridge and Evidence trust domains;
- Customer Evidence inherits Payment environment;
- Provider Evidence inherits authenticated Bridge environment;
- provider candidate queries and relevant uniqueness/dedup semantics must be environment-aware;
- TEST Bridge credentials/evidence must not satisfy LIVE Payments and vice versa.

---

### SEC-035 — Webhook SSRF

**Priority:** HIGH / production blocker  
**Closure status:** `CONFIRMED GAP`

Current webhook URL validation accepts any absolute `http` or `https` host. Current transport sends directly to the configured URI. Redirects are disabled, which is good, but the initial target is not protected.

Required closure:

- reject loopback, private, link-local, multicast, unspecified and reserved destinations;
- reject known metadata endpoints;
- validate host resolution before delivery, not only at configuration time;
- re-resolve/re-check for every attempt to reduce DNS-rebinding exposure;
- require HTTPS in production;
- keep an explicit local-development exception;
- document residual DNS TOCTOU risk until network-level egress control/proxy exists.

---

### SEC-036 — Known Development Webhook Master Secret Fallback

**Priority:** HIGH; CRITICAL if deployed  
**Closure status:** `CONFIRMED GAP`

`WebhookSignatureService` accepts a public development fallback when `VEROX_WEBHOOK_MASTER_SECRET` is absent.

Required production invariant:

```text
PRODUCTION + MISSING/DEFAULT WEBHOOK MASTER SECRET
= STARTUP FAILURE
```

A warning is insufficient.

---

### SEC-037 — Unbounded Merchant-Wide Reverification

**Priority:** MEDIUM  
**Closure status:** `CONFIRMED GAP`

A Provider Evidence event invokes `verifyMerchant()` which loads every active Payment for the merchant and verifies each one.

Required closure direction:

- provider-candidate preselection;
- pagination/batching or bounded work;
- eventual queue/worker model when volume requires it.

This is primarily availability/scaling, not a current false-confirmation path.

---

### SEC-038 — Non-Expiring Unlinked Provider Evidence

**Priority:** HIGH / amplifier of SEC-032  
**Closure status:** `CONFIRMED GAP`

No automatic-match eligibility window exists for unlinked Provider Evidence.

Required closure is part of SEC-032. Evidence may be retained for audit while becoming ineligible for automatic matching; retention and matching eligibility are separate concepts.

---

### SEC-039 — Permissive M-Pesa Parser

**Priority:** HIGH  
**Closure status:** `CONFIRMED HARDENING GAP / ADVERSARIAL TEST REQUIRED`

The current parser uses regex `find()` and extracts a limited reference/amount tuple. Customer parsing is not a full-message template validation. The provider reference is anchored at the start, but amount extraction still accepts the first matching amount token in otherwise broader content.

Required closure:

- real provider-template corpus;
- strict known-template recognition;
- multiple-reference/multiple-amount ambiguity rejection;
- Unicode/control-character adversarial tests;
- deterministic failure on malformed or unknown structures;
- provider-format versioning strategy.

Parser hardening is defense in depth and does not replace Evidence-to-Payment binding.

---

### SEC-040 — Plain HTTP Accepted for Sensitive URLs

**Priority:** MEDIUM  
**Closure status:** `CONFIRMED GAP`

Webhook and merchant redirect URL validation currently permits `http://`.

Required closure:

- production requires HTTPS;
- local development may explicitly allow HTTP.

---

### SEC-041 — Explicit CORS Policy Missing

**Priority:** LOW now / mandatory before Hosted Checkout  
**Closure status:** `CONFIRMED GAP`

`SecurityConfig` does not define CORS.

Required closure belongs before the browser frontend consumes public APIs. Do not solve this with `*` origins under deadline pressure.

---

### SEC-042 — CI Does Not Enforce Tests/Security Checks

**Priority:** HIGH process gate  
**Closure status:** `CONFIRMED GAP`

Current backend CI executes Maven package with `-DskipTests` and has no security scanning baseline.

Required closure:

- full backend tests in CI;
- dependency vulnerability scanning;
- basic SAST/secret scanning strategy;
- SBOM generation or an explicitly scheduled maturity step;
- CI must fail when security-regression tests fail.

---

### SEC-043 — Structured Security Logging / Alerting

**Priority:** HIGH  
**Closure status:** `CONFIRMED OPERATIONS GAP`

Current logging is application-oriented. There is no dedicated structured security event model for authentication failures, evidence anomalies, review reasons, replay attempts or suspicious confirmation patterns.

Required closure can be staged, but production must at minimum produce queryable security-relevant events without logging full sensitive evidence.

---

### SEC-044 — Raw Evidence Retention / Access Policy Undefined

**Priority:** HIGH governance  
**Closure status:** `CONFIRMED GOVERNANCE GAP`

Raw payment SMS content is persisted as text and there is no implemented retention/access policy in the backend snapshot.

Required closure:

- retention period;
- access model;
- audit of privileged reads;
- backup implications;
- encryption-at-rest strategy;
- legal/privacy review.

Do not delete evidence merely to solve automatic-match TTL. Retention and automatic-match eligibility are distinct.

---

### SEC-045 — Bootstrap Credential Logging

**Priority:** MEDIUM  
**Closure status:** `CONFIRMED GAP`

Bootstrap flows emit newly issued plaintext credentials to logs. Bootstraps are disabled by default, which reduces exposure but does not make production log disclosure acceptable.

Required closure:

- production must prohibit bootstrap credential logging/provisioning path;
- one-time credential issuance must use an explicit controlled operations mechanism.

---

### SEC-046 — API/Bridge Credential Rotation and Revocation Operations

**Priority:** CRITICAL operational control  
**Closure status:** `AUDIT-CONFIRMED GAP / implementation review continues`

Credential status is represented in the model and authentication checks active state, but no complete merchant/operations rotation/revocation workflow was identified by the audit or current repository history review.

Required closure:

- emergency revoke;
- rotate;
- list/identify active credentials without exposing secret values;
- audit credential lifecycle events;
- Bridge loss/compromise runbook.

---

### SEC-047 — Bridge Client Provenance Unknown

**Priority:** CRITICAL trust assumption  
**Closure status:** `ARCHITECTURAL RISK`

Server-side Bridge authentication proves possession of a Bridge credential and correct merchant/Bridge scope. It does not prove that the text submitted by the device was generated by an authentic provider event.

Current iOS Shortcut MVP must therefore be explicitly treated as a trusted capture-agent assumption, not cryptographic proof of provider event provenance.

Required closure for MVP:

- document the exact approved Shortcut/automation configuration;
- keep Bridge credential dedicated and revocable;
- restrict the device to the merchant receiving channel;
- prohibit modification of the approved automation;
- define lost/compromised device response;
- schedule native Bridge/client provenance hardening as the product matures.

---

### SEC-048 — Database Uniqueness Race Exception Handling

**Priority:** HIGH  
**Closure status:** `TEST REQUIRED`

PostgreSQL provides a unique index on `(merchant_id, provider, provider_transaction_reference)`. This is a valuable concurrency backstop.

The real concurrent loser path still requires explicit runtime/integration testing to prove safe rollback/state/webhook behavior.

---

### SEC-049 — Customer Evidence Poisoning / Forced REVIEW_REQUIRED

**Priority:** HIGH in Master Plan; **elevated to P0 for current Hosted Checkout design**  
**Closure status:** `CONFIRMED LOGIC GAP / REPRODUCTION REQUIRED`

The public endpoint accepts Customer Evidence using knowledge of the checkout ID. After commit, verification runs automatically.

Current Verification Engine behavior can move the Payment to terminal `REVIEW_REQUIRED` when submitted customer content is unrecognized, amount-mismatched, currency-mismatched or conflicting. `REVIEW_REQUIRED` is outside the current active verification statuses.

Therefore a party with a leaked checkout ID can potentially poison automatic confirmation before the legitimate payer completes the flow.

Required architectural correction:

- invalid/unmatched customer claims must not permanently poison payment truth;
- build candidate **pairs**, not merely reject the entire payment because unrelated Customer Evidence exists;
- only evidence that participates in a real conflicting match should force ambiguity review;
- legitimate later evidence must be able to recover from unrelated garbage submissions;
- combine with checkout-scoped rate limiting and session/capability hardening.

---

### SEC-050 — Public Checkout ID Becomes a Security Capability

**Priority:** HIGH  
**Closure status:** `CONFIRMED DESIGN PROPERTY / SECURITY DESIGN REQUIRED`

The current public evidence-submission endpoint authorizes the operation only through possession of a high-entropy checkout identifier.

High entropy protects enumeration but not leakage.

Required decision before Hosted Checkout production:

- either explicitly treat the checkout URL as a capability URL and protect it accordingly;
- or introduce a distinct submission capability/session token separate from the public object identifier.

The browser must avoid leaking checkout capabilities through analytics, referrers or logs.

---

### SEC-051 — Bridge Shortcut Configuration Integrity

**Priority:** CRITICAL trust assumption  
**Closure status:** `ARCHITECTURAL RISK`

An editable device automation can be changed locally while continuing to possess a valid Bridge credential.

For the MVP, this is not solved by server authentication. It must be documented, operationally constrained and progressively replaced/hardened if VEROX scales beyond a tightly controlled receiving device.

---

### SEC-052 — Client-Controlled Bridge `received_at`

**Category:** Evidence Provenance / Temporal Integrity  
**Priority:** HIGH as a design constraint  
**Status:** `NEW FINDING — CONFIRMED IN CODE`

The Bridge request accepts `received_at` from the client and the ingestion service persists that value when supplied.

Current Verification Engine does not use this field for matching, so this is not presently a false-confirmation exploit by itself.

However, SEC-032/SEC-038 remediation is expected to introduce temporal eligibility. That remediation must **not** treat Bridge-supplied `received_at` as the sole trusted clock.

Required invariant:

```text
client supplied received_at = evidence metadata / claim
server created_at / server ingest time = security-authoritative arrival observation
```

Provider-message embedded transaction time may be another signal when safely parsed, but must be treated according to provider-specific trust rules.

---

## 3. Revised Security Closure Order

### SECURITY WAVE A — PAYMENT TRUTH / TRUTH DENIAL — P0

1. SEC-049 — prevent public Customer Evidence poisoning.
2. SEC-032 + SEC-038 — provider-evidence automatic-match eligibility and evidence-to-payment binding hardening.
3. SEC-034 — TEST/LIVE isolation through Bridge/Evidence/Verification.
4. SEC-050 — checkout submission capability decision.
5. SEC-052 — trusted temporal semantics while implementing the above.
6. SEC-048 — concurrency test once new binding constraints are in place.

### SECURITY WAVE B — PRODUCTION PERIMETER — P0/P1

1. SEC-036 — production webhook master-secret fail-fast.
2. SEC-035 — webhook SSRF destination policy and delivery-time recheck.
3. SEC-033 — rate limiting.
4. SEC-040 — HTTPS-only production URLs.
5. SEC-045 — production bootstrap secret-log prohibition.

### SECURITY WAVE C — VERIFICATION HARDENING — P1

1. SEC-039 — strict parser + adversarial corpus.
2. SEC-046 — key/Bridge revocation and rotation operations.
3. SEC-037 — bound provider-triggered merchant reverification.

### SECURITY WAVE D — SDLC / OPERATIONS / DATA — P1/P2

1. SEC-042 — CI runs tests and security checks.
2. SEC-043 — structured security events.
3. SEC-044 — evidence retention/access policy.
4. SEC-041 — explicit CORS before frontend.
5. SEC-047/SEC-051 — Bridge-client provenance program and risk acceptance for the Shortcut MVP.

---

## 4. Required Reproduction / Regression Tests

The following tests are mandatory evidence before security closure:

- `SEC-RT-001`: orphan/stale Provider Evidence cannot confirm a Checkout created after that evidence arrived.
- `SEC-RT-002`: TEST Payment cannot consume LIVE Provider Evidence; LIVE Payment cannot consume TEST Evidence.
- `SEC-RT-003`: invalid/fake Customer Evidence cannot permanently prevent a later legitimate evidence pair from confirming.
- `SEC-RT-004`: two distinct Customer claims plus only one real Provider pair confirm the uniquely real pair instead of being poisoned by unrelated garbage.
- `SEC-RT-005`: same provider transaction reference raced by two Payments results in at most one confirmation, with the loser failing safely and no duplicate webhook.
- `SEC-RT-006`: production application refuses unsafe/default/missing webhook master secret.
- `SEC-RT-007`: webhook destinations reject loopback/private/link-local/metadata targets and are rechecked at delivery.
- `SEC-RT-008`: public evidence submission returns rate-limit responses after the configured threshold.
- `SEC-RT-009`: adversarial parser corpus fails closed.

---

## 5. Security Closure Gate

Railway staging/production deployment must not be treated as security-closed until:

- all P0 truth-integrity findings are implemented and regression-tested;
- production webhook secret fail-fast is verified;
- basic webhook SSRF protection is verified;
- public evidence rate limiting is verified;
- TEST/LIVE isolation is verified;
- residual Bridge Shortcut provenance risk is explicitly documented/accepted for the controlled MVP device;
- full backend suite passes after security changes.

Frontend implementation must additionally close CORS/capability-leakage requirements before browser production use.

---

## 6. Current Task

`SECURITY-WAVE-A-01 — Redesign Verification candidate selection so unrelated/invalid Customer Evidence cannot poison a legitimate Payment, while preserving fail-closed confirmation and provider-reference single-use semantics.`

Next:

`SECURITY-WAVE-A-02 — Add server-authoritative Provider Evidence eligibility window + environment binding and security regression tests.`
