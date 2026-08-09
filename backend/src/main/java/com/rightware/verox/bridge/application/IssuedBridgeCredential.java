package com.rightware.verox.bridge.application;

import java.util.UUID;

public record IssuedBridgeCredential(
    UUID bridgeId,
    String bridgePublicId,
    UUID credentialId,
    String value,
    String prefix
) {
}
