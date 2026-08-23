package com.rightware.verox.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class HostedCheckoutCorsConfig {

    @Bean
    CorsConfigurationSource corsConfigurationSource(
        @Value("${verox.cors.hosted-checkout-allowed-origins:}") String checkoutOrigins,
        @Value("${verox.cors.merchant-platform-allowed-origins:}") String platformOrigins
    ) {
        CorsConfiguration checkout = new CorsConfiguration();
        checkout.setAllowedOrigins(parseAllowedOrigins(checkoutOrigins));
        checkout.setAllowedMethods(List.of(
            HttpMethod.GET.name(),
            HttpMethod.POST.name(),
            HttpMethod.OPTIONS.name()
        ));
        checkout.setAllowedHeaders(List.of(
            HttpHeaders.CONTENT_TYPE,
            "VEROX-Checkout-Capability"
        ));
        checkout.setAllowCredentials(false);

        CorsConfiguration platform = new CorsConfiguration();
        platform.setAllowedOrigins(parseAllowedOrigins(platformOrigins));
        platform.setAllowedMethods(List.of(
            HttpMethod.GET.name(),
            HttpMethod.POST.name(),
            HttpMethod.OPTIONS.name()
        ));
        platform.setAllowedHeaders(List.of(
            HttpHeaders.CONTENT_TYPE,
            "X-VEROX-CSRF"
        ));
        platform.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/public/v1/checkout/**", checkout);
        source.registerCorsConfiguration("/platform/v1/**", platform);
        return source;
    }

    private List<String> parseAllowedOrigins(String configuredOrigins) {
        if (configuredOrigins == null || configuredOrigins.isBlank()) {
            return List.of();
        }

        List<String> origins = Arrays.stream(configuredOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isBlank())
            .toList();

        if (origins.stream().anyMatch("*"::equals)) {
            throw new IllegalStateException(
                "Wildcard CORS origins are prohibited for VEROX browser applications"
            );
        }

        return origins;
    }
}
