# FRONTEND-DECISION-005 — Merchant Platform Operational Model

Status: APPROVED FRONTEND DECISION

The VEROX Merchant Platform is a dark-first operational console for payment verification / payment trust infrastructure, not a generic fintech dashboard.

The frontend represents distinct state domains for Merchant, Environment, Payment Channel, Checkout Session, Payment / Verification, Review, Webhook Delivery, API Key and Team access. UI state never exceeds VEROX Server truth.

TEST and LIVE are persistent product contexts and must not visually mix. Checkout state remains distinct from payment state. `REVIEW_REQUIRED` is an operational workflow and does not grant the merchant authority to confirm payment. Webhook delivery state is distinct from payment confirmation.

Primary information architecture:

- Operations: Overview, Payments, Checkouts, Reviews
- Payment Infrastructure: Payment Channels
- Developers: Integration, API Keys, Webhooks, API Logs
- Organization: Team, Account, Settings

Backend-incomplete surfaces use typed frontend fallback contracts isolated from presentational components. Fallback data is explicitly marked as TEST preview and is mechanically replaceable by service/query data.

Strategic reusable component: Trust Timeline, representing safe operational events without exposing internal verification algorithms, parser confidence, credentials or secrets.
