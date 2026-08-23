package com.rightware.verox.authentication.application;

import com.rightware.verox.authentication.domain.ApiKeyEnvironment;
import java.util.UUID;

public record MerchantOperatorPrincipal(
    UUID operatorId,
    String operatorDisplayName,
    UUID merchantId,
    String merchantName,
    ApiKeyEnvironment environment,
    UUID sessionId
) {
}
