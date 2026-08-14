package com.rightware.verox.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class ProductionSecretGuard {

    static final String DEVELOPMENT_WEBHOOK_MASTER_SECRET =
        "verox-dev-webhook-master-secret-change-me";

    static final String DEVELOPMENT_CHECKOUT_CAPABILITY_MASTER_SECRET =
        "verox-dev-checkout-capability-master-secret-change-me";

    static final int MINIMUM_PRODUCTION_SECRET_BYTES = 32;

    public ProductionSecretGuard(
        Environment environment,
        @Value("${verox.webhook.master-secret:}") String webhookMasterSecret,
        @Value("${verox.checkout.capability-master-secret:}") String checkoutCapabilityMasterSecret
    ) {
        if (!environment.acceptsProfiles(Profiles.of("production"))) {
            return;
        }

        requireProductionSecret(
            "VEROX_WEBHOOK_MASTER_SECRET",
            webhookMasterSecret,
            DEVELOPMENT_WEBHOOK_MASTER_SECRET
        );

        requireProductionSecret(
            "VEROX_CHECKOUT_CAPABILITY_MASTER_SECRET",
            checkoutCapabilityMasterSecret,
            DEVELOPMENT_CHECKOUT_CAPABILITY_MASTER_SECRET
        );
    }

    private static void requireProductionSecret(
        String environmentVariable,
        String secret,
        String knownDevelopmentDefault
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                environmentVariable
                    + " is required when the production Spring profile is active"
            );
        }

        if (knownDevelopmentDefault.equals(secret.trim())) {
            throw new IllegalStateException(
                environmentVariable
                    + " must not use the known VEROX development default in production"
            );
        }

        if (secret.getBytes(StandardCharsets.UTF_8).length
            < MINIMUM_PRODUCTION_SECRET_BYTES) {
            throw new IllegalStateException(
                environmentVariable
                    + " must contain at least "
                    + MINIMUM_PRODUCTION_SECRET_BYTES
                    + " bytes in production"
            );
        }
    }
}
