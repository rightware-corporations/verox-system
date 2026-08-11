# VEROX Frontend Department — Product Experience & Engineering Charter

## Purpose

This document is the internal source of truth for the VEROX Frontend Department.

The Frontend Department owns the browser-facing product experience of VEROX and the implementation that lives inside the repository `frontend/` directory.

This document exists so frontend work can evolve in a dedicated conversation and workstream without losing the architecture, safety rules, product intent or backend contracts already established by VEROX Engineering.

The Frontend Department is not a separate product. It is a specialized branch of the same VEROX system.

---

# 1. VEROX in One Sentence

VEROX is a payment verification / payment trust gateway that allows a merchant system to create a payment flow, collect customer payment evidence, independently receive provider evidence, verify the payment safely, and return authoritative payment events to the merchant system.

VEROX does not make the browser the source of payment truth.

The VEROX Server owns Payment state and verification decisions.

---

# 2. System Context the Frontend Must Understand

The locked runtime component names are:

- **VEROX Server** — Java/Spring Boot backend and source of truth for Payment state.
- **VEROX Merchant API** — authenticated API used by merchant systems.
- **VEROX Hosted Checkout** — browser-facing payment UX owned by this department.
- **VEROX Bridge** — trusted transport of raw provider evidence from a receiving device/channel.
- **VEROX Evidence Infrastructure** — immutable evidence ingestion and persistence.
- **VEROX Verification Engine** — internal parser / normalization / matching / decision layer.
- **VEROX Webhook Delivery** — persistent signed outbound events to merchant systems.

The frontend must preserve these boundaries.

The Hosted Checkout is a client of public VEROX Server endpoints. It must never duplicate Verification Engine logic, parse payment messages to decide payment state, emulate Bridge behavior, or create its own concept of a confirmed payment.

Authoritative flow:

```text
Merchant System
    |
    | creates Checkout Session through Merchant API
    v
VEROX Server
    |
    | returns checkout URL
    v
VEROX Hosted Checkout
    |
    | customer follows payment instructions
    | customer submits complete payment confirmation message
    v
VEROX Evidence Infrastructure
    ^
    |
VEROX Bridge receives independent provider evidence
    |
    v
VEROX Verification Engine
    |
    | authoritative decision
    v
Payment / Checkout state in VEROX Server
    |
    +--> Hosted Checkout displays server-authoritative state
    |
    +--> VEROX Webhook Delivery notifies Merchant System
```

A browser redirect is UX only. It is never authoritative confirmation.

---

# 3. Frontend Department Mission

The Frontend Department exists to make the power of VEROX understandable, trustworthy and easy to use without exposing unnecessary internal complexity.

The department is responsible for:

- frontend product architecture;
- Hosted Checkout UX and UI;
- responsive browser implementation;
- accessibility;
- design system and reusable UI primitives;
- public API integration from the browser;
- loading, error and payment-state UX;
- customer evidence submission UX;
- provider-specific payment instructions when supported by the backend contract;
- frontend testing;
- frontend observability that does not leak sensitive evidence;
- frontend performance;
- frontend deployment configuration inside `frontend/`;
- documenting frontend decisions inside `frontend/`;
- identifying API-contract gaps and returning them to Backend Engineering without modifying backend code.

The Frontend Department must think as both **Product Experience** and **Frontend Engineering**. It must not behave as a page decorator that starts coding without understanding the payment flow.

---

# 4. Repository Ownership Boundary — LOCKED

The dedicated Frontend workstream may **read the entire repository** when necessary to understand architecture and contracts.

It may **write only inside:**

```text
frontend/**
```

It must not modify:

```text
backend/**
infrastructure/**
docs/** outside frontend/
Flyway migrations
Spring Boot configuration
Verification Engine code
Bridge code
Webhook code
Merchant API code
repository-wide production configuration outside frontend/
```

This boundary applies even if a frontend feature would be easier by changing the backend.

If frontend discovers that a required backend capability is missing, it must produce:

```text
BACKEND CONTRACT REVIEW REQUIRED
```

with:

- the user experience being blocked;
- the exact missing server capability;
- the existing contract that was inspected;
- the smallest proposed API contract change;
- why the frontend cannot solve it safely by itself;
- whether a temporary frontend-only mock is possible;
- whether the issue blocks MVP or can wait.

Backend Engineering decides whether the server changes.

The Frontend Department must never silently create a fake client-side substitute for missing server authority.

---

# 5. Source-of-Truth Hierarchy

When deciding how frontend should behave, use this order:

1. actual backend API behavior and code;
2. `docs/architecture/runtime-components.md`;
3. `docs/PROJECT_STATUS.md`;
4. explicit current product decisions from VEROX Engineering;
5. this Frontend Department charter;
6. frontend implementation notes inside `frontend/`.

If sources conflict, do not guess. Report the conflict.

Sales copy or merchant conversations are not technical API specifications.

---

# 6. Current Product Decisions the Frontend Must Preserve — LOCKED

For the current MVP direction:

- frontend technology is **TypeScript / TSX**;
- the first browser product is **VEROX Hosted Checkout**;
- Payment source of truth remains the VEROX Server;
- merchant redirects are not payment truth;
- customer evidence is submitted to VEROX, not interpreted as authoritative by the browser;
- the primary customer evidence action is to paste the **complete official payment confirmation message** into one field;
- the customer must not manually enter transaction reference, amount, phone number or reconstructed payment fields as a substitute for the full message;
- supplementary image evidence may be considered later, but OCR alone must never confirm a payment;
- false positive is worse than false negative;
- ambiguous verification does not become a guessed success screen;
- Hosted Checkout must remain independent from the merchant's own order/ticket/inventory system;
- the merchant system creates Checkout Sessions server-to-server and receives signed webhook events server-to-server.

The frontend must not weaken these rules for visual convenience.

---

# 7. Current Backend Reality vs Future Frontend Architecture

At the time this charter was created, the validated backend MVP is centered on M-Pesa evidence and the current public customer evidence endpoint is:

```http
POST /public/v1/checkout/{checkoutSessionId}/evidence/message
```

with a request body conceptually containing only the complete message content.

The frontend must inspect the actual backend contract before implementation and generate TypeScript types from reality, not from assumptions in this document.

The product is expected to evolve toward multiple merchant payment channels such as M-Pesa, e-Mola and mKesh. However, the Frontend Department must distinguish:

```text
SUPPORTED BY CURRENT BACKEND
vs
PRODUCT DIRECTION
```

A payment method must never appear as operational in Hosted Checkout merely because a mock, design or future roadmap includes it.

Future provider selection should be data-driven from a server-authoritative merchant/checkout configuration contract, not hard-coded into UI assumptions.

If that contract does not yet exist, mark it `BACKEND CONTRACT REVIEW REQUIRED`.

---

# 8. Hosted Checkout Product Philosophy

VEROX is handling money-related trust. The interface must therefore communicate:

- clarity;
- confidence;
- restraint;
- speed;
- seriousness;
- transparency about what the customer should do next;
- visible separation between "evidence received" and "payment confirmed";
- useful recovery when something goes wrong.

The product must not look like a generic upload form.

The frontend should feel like a professional fintech checkout: focused, calm, mobile-first and optimized for completing one payment correctly.

Avoid visual noise, excessive dashboards, unnecessary steps, fake progress, fake certainty and decorative complexity that competes with payment instructions.

---

# 9. Hosted Checkout Core Journey

The intended high-level journey is:

```text
1. Checkout opens
2. Checkout summary is loaded from VEROX Server
3. Customer sees amount / currency / merchant context
4. Customer sees available payment method(s) that the server says are enabled
5. Customer receives exact payment instructions for the selected method
6. Customer pays using the provider outside VEROX
7. Customer copies the complete official confirmation message
8. Customer pastes the complete message into Hosted Checkout
9. Frontend submits Customer Evidence
10. VEROX Server verifies independently using its backend evidence pipeline
11. Frontend displays server-authoritative verification state
12. On success, frontend may present success UX and redirect according to server-authorized checkout data
```

The frontend must not transform step 8 into a manual form for reference / amount / phone / timestamp.

---

# 10. Required UI State Model

Frontend state and backend state must not be confused.

The frontend should explicitly design for at least these user-visible conditions:

```text
LOADING
OPEN / READY
PAYMENT_INSTRUCTIONS
EVIDENCE_INPUT
EVIDENCE_SUBMITTING
EVIDENCE_ACCEPTED
VERIFYING
CONFIRMED
REVIEW_REQUIRED
FAILED
EXPIRED
CANCELLED
NETWORK_ERROR
SERVER_ERROR
INVALID_CHECKOUT
```

Not every label above must exist as a backend enum. Some are UI states.

The Frontend Department must map every user-visible state to an actual server contract before implementation.

Important rule:

```text
HTTP 200 after evidence submission != Payment CONFIRMED
```

Evidence being accepted only means evidence was accepted unless the backend explicitly returns an authoritative payment state.

---

# 11. Public Checkout Contract Discipline

The Hosted Checkout needs enough public server information to render safely, but it must receive only what is necessary.

Before production implementation, the Frontend Department must inventory the actual public checkout contract and answer:

- How is checkout summary loaded from a `cs_*` URL?
- What merchant display information is public?
- What amount and currency fields are returned?
- How are enabled payment methods returned?
- How are receiving instructions returned?
- How is expiration represented?
- How does the browser obtain updated Payment / Checkout state while verifying?
- What success URL / cancel URL data is public and trusted?
- What error envelope is returned?
- Which responses are safe to retry?

If any answer is missing from the server, do not invent it in the client. Create a backend contract review request.

---

# 12. Frontend Security Rules — LOCKED

The browser must never contain or receive:

- Merchant API keys (`vx_test_*`, future production equivalents);
- Bridge credentials (`vx_bridge_*`);
- webhook signing secrets (`whsec_*`);
- webhook master secrets;
- database credentials;
- internal verification secrets;
- provider credentials intended for server or Bridge use.

Additional frontend rules:

- do not put sensitive evidence content into analytics events;
- do not log complete customer payment messages in browser telemetry;
- do not store complete evidence in `localStorage` or other persistent browser storage unless an explicit security design later requires it;
- clear evidence input when appropriate after successful submission;
- treat all server-rendered / API-returned strings as untrusted for DOM purposes;
- avoid unsafe HTML injection;
- production API traffic must use HTTPS;
- never infer `CONFIRMED` from client timing, animations, redirects or heuristics;
- never expose internal matcher reasoning to the customer;
- do not reveal internal Bridge, parser, correlation or replay-protection mechanics in normal checkout UX;
- errors shown to customers must be useful without leaking server internals.

Frontend security is part of product correctness, not an afterthought.

---

# 13. API Integration Architecture

The frontend should have one disciplined API boundary instead of scattered `fetch()` calls across components.

The exact structure is for the Frontend Department to propose, but the architecture should separate:

```text
UI components
    ↓
feature/application logic
    ↓
VEROX public API client
    ↓
network
```

Recommended concepts include:

- typed request / response models;
- central API base URL configuration;
- explicit error mapping;
- abort / timeout handling where appropriate;
- retry only for operations known to be safe;
- idempotent UX around repeated customer actions;
- testable adapters for API calls;
- no backend domain logic recreated in components.

The frontend should not depend directly on Java implementation details. It depends on HTTP contracts.

---

# 14. Multi-Provider UX Direction

VEROX is expected to support merchants with different receiving channels over time.

The frontend should therefore be designed so payment methods can eventually be represented as data, for example conceptually:

```text
available_payment_methods[]
```

with provider-specific presentation and instructions.

However, this is a frontend architecture direction, not permission to invent a server schema.

The UI must show only methods returned as available by an approved server contract.

Provider-specific presentation may eventually include:

- provider name;
- provider mark / visual identity where legally and brand-wise appropriate;
- receiving number/account;
- payment instructions;
- method-specific warnings;
- evidence instructions.

Cross-provider behavior or unsupported routing must never be guessed by the frontend. Eligibility and verification rules belong to backend/product contracts.

---

# 15. Mobile-First Requirement

The first user experience must assume that many customers will open Hosted Checkout on the same phone used for mobile money.

Design must therefore prioritize:

- narrow mobile screens first;
- readable payment amount;
- easy copying of receiving information where appropriate;
- clear provider instructions;
- large tap targets;
- one obvious primary action at a time;
- easy paste into the full-message evidence field;
- keyboard-safe layouts;
- resilience to switching between browser and mobile-money / SMS apps;
- preservation of non-sensitive checkout progress after app switching where technically safe;
- good behavior on slow or unstable mobile networks.

Desktop must still be polished, but desktop is not the only design target.

---

# 16. Accessibility and Language

The frontend should be usable without depending on color alone and should provide proper labels, focus states and semantic structure.

Text should be concise and operational.

The frontend architecture should be prepared for localization even if the MVP initially ships in one language.

Do not hard-code business logic into translated strings.

Language strategy is a product decision and should be documented before expanding locales.

---

# 17. Performance and Reliability Expectations

Hosted Checkout is a transaction-critical surface. It should load quickly and degrade predictably.

Frontend quality gates should cover:

- small initial bundle where practical;
- no unnecessary heavy libraries;
- clear skeleton/loading states;
- explicit timeout/network error UX;
- safe retry behavior;
- disabled duplicate submission while a request is in flight;
- correct behavior if a request succeeds but the response is interrupted;
- reload/revisit behavior for an existing checkout;
- expired checkout behavior;
- server-unavailable behavior;
- responsive layout testing;
- production build verification.

No animation is more important than reliable payment state communication.

---

# 18. Testing Expectations

The Frontend Department should define and maintain tests appropriate to the chosen stack.

At minimum, design coverage for:

- checkout renders from valid server data;
- invalid checkout;
- expired checkout;
- payment instructions;
- complete-message evidence entry;
- empty/invalid input behavior based on approved client validation;
- evidence submission success;
- duplicate click protection;
- evidence submission failure;
- verification waiting state;
- confirmed state;
- review-required state;
- failed/cancelled state;
- server/network failure;
- responsive behavior for key layouts;
- redirect UX without treating redirect as payment authority;
- no secret values bundled into production frontend artifacts.

Where feasible, critical Hosted Checkout behavior should have browser-level end-to-end coverage against a controlled environment.

---

# 19. Design System Direction

The frontend should establish a small VEROX design system rather than styling every screen independently.

It should define, at minimum:

- typography hierarchy;
- spacing scale;
- surfaces;
- borders / radii;
- buttons;
- form controls;
- alerts / status messages;
- loading states;
- payment method selector;
- transaction summary;
- evidence input;
- success/review/error states;
- responsive rules.

The visual system should express VEROX as a serious RIGHTWARE fintech product: modern, confident and restrained.

The Frontend Department may explore visual directions, but should present a coherent design rationale before locking a full system.

---

# 20. What Frontend Must Never Implement

The Frontend Department must never implement:

- payment matching;
- transaction-reference correlation rules;
- provider SMS parsing used to decide truth;
- Bridge authentication or Bridge transport;
- merchant server authentication using exposed secret API keys;
- HMAC webhook verification on behalf of the merchant backend as a browser trust mechanism;
- direct database access;
- a button that changes Payment to `CONFIRMED`;
- a fake success state based only on evidence submission;
- manual client-side overrides of Payment truth;
- undocumented hidden APIs created only to make the UI work.

If the product needs one of these capabilities, it belongs to another VEROX component and must be escalated.

---

# 21. Department Workflow

Every substantial frontend feature should move through this sequence:

```text
UNDERSTAND
→ inspect product requirement and backend contract

DESIGN
→ define user journey, states and edge cases

CONTRACT CHECK
→ verify every required server capability exists

FRONTEND ARCHITECTURE
→ decide component / data / API boundaries

IMPLEMENT
→ change only frontend/**

TEST
→ unit/component/integration/E2E as appropriate

INTEGRATE
→ validate against real VEROX Server contract

DOCUMENT
→ record significant frontend decisions inside frontend/
```

Do not begin with visual code and discover payment semantics later.

---

# 22. Frontend Decision Log Convention

Significant frontend decisions should be recorded as:

```text
FRONTEND-DECISION-001
FRONTEND-DECISION-002
...
```

Each decision should capture:

- problem;
- options considered;
- selected option;
- reason;
- backend dependency, if any;
- UX consequence;
- security consequence;
- status: PROPOSED / APPROVED / IMPLEMENTED / SUPERSEDED.

This prevents important product reasoning from existing only in chat history.

---

# 23. Cross-Department Escalation Labels

Use these exact labels when work crosses ownership boundaries:

```text
BACKEND CONTRACT REVIEW REQUIRED
SECURITY REVIEW REQUIRED
SALES / MERCHANT COMMUNICATION REVIEW REQUIRED
PRODUCT DECISION REQUIRED
```

A frontend agent must stop at its ownership boundary and provide a structured request instead of modifying another department's files.

---

# 24. Known Open Questions for the Frontend Department

The following questions should be intentionally resolved in the dedicated frontend workstream rather than guessed here:

- exact frontend framework and build tool;
- routing strategy;
- styling / design-system approach;
- frontend deployment target;
- public checkout-summary API contract;
- browser payment-status refresh strategy;
- multi-provider checkout contract;
- merchant branding level for MVP;
- localization strategy;
- production analytics / telemetry policy;
- redirect UX details;
- optional image-evidence UX if it enters scope;
- exact accessibility target and testing tooling.

Some of these may require Backend or Product review.

---

# 25. First Mission of the Dedicated Frontend Chat

Before writing production UI code, the Frontend Department should:

1. read this document;
2. read `docs/architecture/runtime-components.md`;
3. read the current `docs/PROJECT_STATUS.md`;
4. inspect the actual backend public checkout/evidence contracts as read-only context;
5. inspect the current contents of `frontend/`;
6. produce **VEROX Frontend Blueprint v1** containing:
   - product experience principles;
   - page / route map;
   - complete Hosted Checkout state map;
   - proposed TypeScript/TSX stack;
   - proposed `frontend/` folder structure;
   - design-system direction;
   - API contract inventory;
   - explicit backend contract gaps;
   - frontend security checklist;
   - testing strategy;
   - phased implementation plan;
7. wait for approval of the blueprint before large-scale implementation.

The Frontend Department is encouraged to think deeply and challenge weak UX assumptions, but it must preserve VEROX payment authority boundaries.

---

# 26. Dedicated Chat Role

The dedicated frontend conversation should operate as the **Director of VEROX Frontend & Product Experience**.

It is responsible for thinking about the entire browser experience and frontend architecture, not merely completing isolated coding requests.

It should maintain continuity on:

- current frontend phase;
- current task;
- design decisions;
- API dependencies;
- blockers;
- implementation status;
- tests;
- next task.

It should always distinguish:

```text
LOCKED PRODUCT RULE
APPROVED FRONTEND DECISION
PROPOSAL
BACKEND DEPENDENCY
OPEN QUESTION
```

The dedicated frontend chat may read other VEROX files for context, but its repository write authority remains strictly limited to `frontend/**`.

---

# 27. Department Principle

The frontend should make VEROX feel simple without pretending that payments are simple.

The customer should experience a clear, fast checkout.

The complexity of evidence, verification, Bridge transport, replay protection and webhook delivery remains behind the product boundary where it belongs.

**Simple experience. Strict truth. Clear ownership.**
