# VEROX Architecture

VEROX is the source of truth for the payment domain.

The integrating merchant system owns its order, ticket, customer and inventory domains. It creates a VEROX checkout session through the public API, redirects the customer to the VEROX Hosted Checkout, and updates its local order state only after receiving and validating a signed VEROX webhook.

## MVP components

- VEROX Core API — Java / Spring Boot
- PostgreSQL — authoritative data store
- VEROX Hosted Checkout — TypeScript / TSX
- Customer Evidence ingestion
- Provider Evidence ingestion
- M-Pesa parser
- Matching Engine
- Webhook Engine
- iOS Shortcut Bridge for the first merchant

## Core rule

External systems cannot set a VEROX payment to `CONFIRMED`. Only VEROX verification logic can produce that state.
