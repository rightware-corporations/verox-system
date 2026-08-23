package com.rightware.verox.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HostedCheckoutCorsConfigTest {

    @Test
    void allowsOnlyConfiguredHostedCheckoutOrigin() {
        HostedCheckoutCorsConfig config = new HostedCheckoutCorsConfig();
        CorsConfigurationSource source = config.corsConfigurationSource(
            "https://checkout.verox.example",
            ""
        );

        MockHttpServletRequest request = new MockHttpServletRequest(
            "OPTIONS",
            "/public/v1/checkout/cs_test/evidence/message"
        );

        CorsConfiguration cors = source.getCorsConfiguration(request);

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOrigins()).containsExactly("https://checkout.verox.example");
        assertThat(cors.getAllowedMethods()).containsExactly("GET", "POST", "OPTIONS");
        assertThat(cors.getAllowedHeaders()).containsExactly("Content-Type", "VEROX-Checkout-Capability");
        assertThat(cors.getAllowCredentials()).isFalse();
    }

    @Test
    void allowsCredentialedMerchantPlatformOrigin() {
        HostedCheckoutCorsConfig config = new HostedCheckoutCorsConfig();
        CorsConfigurationSource source = config.corsConfigurationSource(
            "",
            "https://merchant.verox.example"
        );

        MockHttpServletRequest request = new MockHttpServletRequest(
            "OPTIONS",
            "/platform/v1/auth/session"
        );

        CorsConfiguration cors = source.getCorsConfiguration(request);

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOrigins()).containsExactly("https://merchant.verox.example");
        assertThat(cors.getAllowedMethods()).containsExactly("GET", "POST", "OPTIONS");
        assertThat(cors.getAllowedHeaders()).containsExactly("Content-Type", "X-VEROX-CSRF");
        assertThat(cors.getAllowCredentials()).isTrue();
    }

    @Test
    void failsClosedWhenNoOriginConfigured() {
        HostedCheckoutCorsConfig config = new HostedCheckoutCorsConfig();
        CorsConfigurationSource source = config.corsConfigurationSource("", "");

        MockHttpServletRequest request = new MockHttpServletRequest(
            "OPTIONS",
            "/public/v1/checkout/cs_test/evidence/message"
        );

        CorsConfiguration cors = source.getCorsConfiguration(request);

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOrigins()).isEmpty();
    }

    @Test
    void doesNotExposeMerchantOrBridgeApisToCors() {
        HostedCheckoutCorsConfig config = new HostedCheckoutCorsConfig();
        CorsConfigurationSource source = config.corsConfigurationSource(
            "https://checkout.verox.example",
            "https://merchant.verox.example"
        );

        MockHttpServletRequest merchant = new MockHttpServletRequest("OPTIONS", "/v1/payments/pay_test");
        MockHttpServletRequest bridge = new MockHttpServletRequest("OPTIONS", "/v1/bridges/brg_test/evidence");

        assertThat(source.getCorsConfiguration(merchant)).isNull();
        assertThat(source.getCorsConfiguration(bridge)).isNull();
    }

    @Test
    void rejectsWildcardOriginConfiguration() {
        HostedCheckoutCorsConfig config = new HostedCheckoutCorsConfig();

        assertThatThrownBy(() -> config.corsConfigurationSource("*", ""))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Wildcard CORS origins are prohibited");
        assertThatThrownBy(() -> config.corsConfigurationSource("", "*"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Wildcard CORS origins are prohibited");
    }
}
