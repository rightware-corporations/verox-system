package com.rightware.verox.bridge.application;

import java.util.UUID;

public record BridgePrincipal(
    UUID bridgeId,
    String bridgePublicId,
    UUID merchantId,
    String provider
) {
}
