# VEROX Hosted Checkout

This directory contains the customer-facing hosted checkout for VEROX.

MVP flow:

1. Load a checkout session from the VEROX Core API.
2. Show the payment summary and available payment channel.
3. Present M-Pesa payment instructions.
4. Accept customer evidence upload.
5. Show the verification state.
6. Redirect to the merchant success or cancel URL after the VEROX flow reaches the appropriate state.

The merchant system does not own the payment UI or payment verification logic.
