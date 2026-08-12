package com.rightware.verox.bridge.application;

import com.rightware.verox.authentication.domain.ApiKeyEnvironment;

import java.util.UUID;

public record BridgePrincipal(
    UUID bridgeId,
    String bridgePublicId,
    UUID merchantId,
    ApiKeyEnvironment environment,
    String provider
) {
}
