package com.rightware.verox.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionSecretGuardTest {

    private static final String STRONG_WEBHOOK_SECRET =
        "webhook-production-secret-0123456789abcdef";

    private static final String STRONG_CHECKOUT_SECRET =
        "checkout-production-secret-0123456789abcdef";

    @Test
    void allowsDevelopmentDefaultsOutsideProduction() {
        MockEnvironment environment = new MockEnvironment();

        assertThatCode(() -> new ProductionSecretGuard(
            environment,
            ProductionSecretGuard.DEVELOPMENT_WEBHOOK_MASTER_SECRET,
            ProductionSecretGuard.DEVELOPMENT_CHECKOUT_CAPABILITY_MASTER_SECRET
        )).doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingWebhookSecretInProduction() {
        assertThatThrownBy(() -> new ProductionSecretGuard(
            productionEnvironment(),
            "",
            STRONG_CHECKOUT_SECRET
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("VEROX_WEBHOOK_MASTER_SECRET");
    }

    @Test
    void rejectsMissingCheckoutSecretInProduction() {
        assertThatThrownBy(() -> new ProductionSecretGuard(
            productionEnvironment(),
            STRONG_WEBHOOK_SECRET,
            ""
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("VEROX_CHECKOUT_CAPABILITY_MASTER_SECRET");
    }

    @Test
    void rejectsKnownWebhookDevelopmentDefaultInProduction() {
        assertThatThrownBy(() -> new ProductionSecretGuard(
            productionEnvironment(),
            ProductionSecretGuard.DEVELOPMENT_WEBHOOK_MASTER_SECRET,
            STRONG_CHECKOUT_SECRET
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("development default");
    }

    @Test
    void rejectsKnownCheckoutDevelopmentDefaultInProduction() {
        assertThatThrownBy(() -> new ProductionSecretGuard(
            productionEnvironment(),
            STRONG_WEBHOOK_SECRET,
            ProductionSecretGuard.DEVELOPMENT_CHECKOUT_CAPABILITY_MASTER_SECRET
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("development default");
    }

    @Test
    void rejectsShortProductionSecrets() {
        assertThatThrownBy(() -> new ProductionSecretGuard(
            productionEnvironment(),
            "too-short",
            STRONG_CHECKOUT_SECRET
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("at least 32 bytes");

        assertThatThrownBy(() -> new ProductionSecretGuard(
            productionEnvironment(),
            STRONG_WEBHOOK_SECRET,
            "too-short"
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("at least 32 bytes");
    }

    @Test
    void acceptsStrongProductionSecrets() {
        assertThatCode(() -> new ProductionSecretGuard(
            productionEnvironment(),
            STRONG_WEBHOOK_SECRET,
            STRONG_CHECKOUT_SECRET
        )).doesNotThrowAnyException();
    }

    private MockEnvironment productionEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");
        return environment;
    }
}