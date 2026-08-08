package com.rightware.verox.authentication.application;

import com.rightware.verox.authentication.domain.ApiKeyEnvironment;

import java.util.UUID;

public record IssuedApiKey(
    UUID id,
    String value,
    String prefix,
    ApiKeyEnvironment environment
) {
}
