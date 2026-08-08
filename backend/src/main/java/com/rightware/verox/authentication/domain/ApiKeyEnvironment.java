package com.rightware.verox.authentication.domain;

public enum ApiKeyEnvironment {
    TEST,
    LIVE;

    public String tokenPrefix() {
        return this == LIVE ? "vx_live_" : "vx_test_";
    }
}
