package com.rightware.verox.authentication.application;

import com.rightware.verox.authentication.domain.ApiKeyEnvironment;

import java.util.UUID;

public record MerchantPrincipal(
    UUID merchantId,
    String merchantName,
    UUID apiKeyId,
    ApiKeyEnvironment environment
) {
}
