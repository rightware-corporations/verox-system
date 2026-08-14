package com.rightware.verox.webhook.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebhookDestinationPolicyTest {

    @Test
    void productionAllowsPublicHttpsDestination() {
        WebhookDestinationPolicy policy =
            new WebhookDestinationPolicy(productionEnvironment(), false);

        assertThat(policy.validate("https://8.8.8.8/webhooks/verox"))
            .isEqualTo("https://8.8.8.8/webhooks/verox");
    }

    @Test
    void productionRejectsHttpEvenForPublicDestination() {
        WebhookDestinationPolicy policy =
            new WebhookDestinationPolicy(productionEnvironment(), true);

        assertThatThrownBy(() ->
            policy.validate("http://8.8.8.8/webhooks/verox"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("HTTPS");
    }

    @Test
    void productionRejectsLoopbackAndPrivateDestinations() {
        WebhookDestinationPolicy policy =
            new WebhookDestinationPolicy(productionEnvironment(), false);

        String[] rejected = {
            "https://127.0.0.1/webhook",
            "https://10.0.0.1/webhook",
            "https://172.16.0.1/webhook",
            "https://192.168.1.1/webhook",
            "https://169.254.169.254/latest/meta-data",
            "https://100.64.0.1/webhook"
        };

        for (String url : rejected) {
            assertThatThrownBy(() -> policy.validate(url))
                .as(url)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not public");
        }
    }

    @Test
    void productionRejectsReservedAndDocumentationRanges() {
        WebhookDestinationPolicy policy =
            new WebhookDestinationPolicy(productionEnvironment(), false);

        String[] rejected = {
            "https://0.0.0.0/webhook",
            "https://192.0.2.1/webhook",
            "https://198.18.0.1/webhook",
            "https://198.51.100.1/webhook",
            "https://203.0.113.1/webhook",
            "https://224.0.0.1/webhook",
            "https://240.0.0.1/webhook"
        };

        for (String url : rejected) {
            assertThatThrownBy(() -> policy.validate(url))
                .as(url)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not public");
        }
    }

    @Test
    void productionRejectsLocalhost() {
        WebhookDestinationPolicy policy =
            new WebhookDestinationPolicy(productionEnvironment(), false);

        assertThatThrownBy(() -> policy.validate("https://localhost/webhook"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not public");
    }

    @Test
    void productionRejectsIpv6UniqueLocalAndDocumentationAddresses() {
        WebhookDestinationPolicy policy =
            new WebhookDestinationPolicy(productionEnvironment(), false);

        assertThatThrownBy(() -> policy.validate("https://[fd00:ec2::254]/webhook"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not public");

        assertThatThrownBy(() -> policy.validate("https://[2001:db8::1]/webhook"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not public");
    }

    @Test
    void localDevelopmentExceptionAllowsHttpLoopbackExplicitly() {
        WebhookDestinationPolicy policy =
            new WebhookDestinationPolicy(new MockEnvironment(), true);

        assertThat(policy.validate("http://127.0.0.1:8787/webhook"))
            .isEqualTo("http://127.0.0.1:8787/webhook");
    }

    @Test
    void localDevelopmentExceptionIsNotImplicit() {
        WebhookDestinationPolicy policy =
            new WebhookDestinationPolicy(new MockEnvironment(), false);

        assertThatThrownBy(() ->
            policy.validate("http://127.0.0.1:8787/webhook"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private MockEnvironment productionEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");
        return environment;
    }
}