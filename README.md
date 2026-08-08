# VEROX System

VEROX is RIGHTWARE's payment verification gateway platform. This repository contains the backend, hosted checkout frontend, infrastructure, and technical documentation for the VEROX MVP.

## Repository structure

```text
verox-system/
├── backend/                 # Java / Spring Boot core platform and public API
├── frontend/
│   ├── checkout/            # VEROX Hosted Checkout (TypeScript / TSX)
│   └── dashboard/           # Reserved for future merchant/admin dashboard
├── docs/
│   ├── architecture/        # Architecture decisions and system design
│   └── api/                 # Public API and webhook contracts
├── infrastructure/          # Docker, deployment and environment configuration
└── README.md
```

## Current MVP scope

- M-Pesa as the first payment provider
- Hosted VEROX Checkout
- Checkout Session API
- Customer evidence upload
- Provider evidence ingestion through an iOS Shortcut bridge
- Payment matching and verification
- Signed webhooks back to the merchant system
- PostgreSQL as the payment source of truth

## Architecture principle

VEROX owns the payment domain and is the source of truth for checkout sessions, payments, evidence, verification and payment state. Integrating systems create checkout sessions through the API, redirect customers to the hosted checkout, and react to signed VEROX webhooks.

## Delivery mode

The four-day MVP is implemented directly on `main` with small, focused commits.
