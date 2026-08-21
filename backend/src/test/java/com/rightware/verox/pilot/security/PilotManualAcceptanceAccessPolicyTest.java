package com.rightware.verox.pilot.security;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PilotManualAcceptanceAccessPolicyTest {

    @Test
    void deniesEveryoneWhenConfigurationIsEmpty() {
        PilotManualAcceptanceAccessPolicy policy =
            new PilotManualAcceptanceAccessPolicy("");

        assertThat(policy.isAllowed(UUID.randomUUID())).isFalse();
    }

    @Test
    void allowsOnlyExplicitlyConfiguredMerchant() {
        UUID allowed = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        PilotManualAcceptanceAccessPolicy policy =
            new PilotManualAcceptanceAccessPolicy(allowed.toString());

        assertThat(policy.isAllowed(allowed)).isTrue();
        assertThat(policy.isAllowed(other)).isFalse();
    }

    @Test
    void supportsExplicitCommaSeparatedAllowlist() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        PilotManualAcceptanceAccessPolicy policy =
            new PilotManualAcceptanceAccessPolicy(first + ", " + second);

        assertThat(policy.isAllowed(first)).isTrue();
        assertThat(policy.isAllowed(second)).isTrue();
    }

    @Test
    void rejectsInvalidMerchantIdConfiguration() {
        assertThatThrownBy(() ->
            new PilotManualAcceptanceAccessPolicy("not-a-uuid")
        )
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Invalid VEROX pilot manual acceptance merchant ID configuration");
    }
}
