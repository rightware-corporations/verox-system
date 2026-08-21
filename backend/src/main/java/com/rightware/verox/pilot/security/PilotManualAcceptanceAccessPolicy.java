package com.rightware.verox.pilot.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class PilotManualAcceptanceAccessPolicy {

    private final Set<UUID> allowedMerchantIds;

    public PilotManualAcceptanceAccessPolicy(
        @Value("${verox.pilot.manual-acceptance-merchant-ids:}") String configuredMerchantIds
    ) {
        this.allowedMerchantIds = parseAllowedMerchantIds(configuredMerchantIds);
    }

    public boolean isAllowed(UUID merchantId) {
        return merchantId != null && allowedMerchantIds.contains(merchantId);
    }

    private Set<UUID> parseAllowedMerchantIds(String configuredMerchantIds) {
        if (configuredMerchantIds == null || configuredMerchantIds.isBlank()) {
            return Set.of();
        }

        try {
            return Arrays.stream(configuredMerchantIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(UUID::fromString)
                .collect(Collectors.toUnmodifiableSet());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                "Invalid VEROX pilot manual acceptance merchant ID configuration",
                exception
            );
        }
    }
}
