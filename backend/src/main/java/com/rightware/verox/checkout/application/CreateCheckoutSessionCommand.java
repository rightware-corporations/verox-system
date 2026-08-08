package com.rightware.verox.checkout.application;

import com.rightware.verox.authentication.domain.ApiKeyEnvironment;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateCheckoutSessionCommand(
    UUID merchantId,
    ApiKeyEnvironment environment,
    String idempotencyKey,
    BigDecimal amount,
    String currency,
    String externalReference,
    String description,
    String successUrl,
    String cancelUrl
) {
}
