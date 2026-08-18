package com.rightware.verox.checkout.application;

import com.rightware.verox.common.web.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RedirectUrlValidatorTest {

    @Test
    void productionAllowsHttpsRedirect() {
        RedirectUrlValidator validator =
            new RedirectUrlValidator(productionEnvironment(), false);

        assertThat(
            validator.validate(
                "https://merchant.example/payment/success",
                "success_url"
            )
        ).isEqualTo(
            "https://merchant.example/payment/success"
        );
    }

    @Test
    void productionRejectsHttpEvenWhenLocalExceptionRequested() {
        RedirectUrlValidator validator =
            new RedirectUrlValidator(productionEnvironment(), true);

        assertThatThrownBy(() ->
            validator.validate(
                "http://merchant.example/payment/success",
                "success_url"
            )
        )
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("HTTPS");
    }

    @Test
    void localHttpExceptionIsNotImplicit() {
        RedirectUrlValidator validator =
            new RedirectUrlValidator(new MockEnvironment(), false);

        assertThatThrownBy(() ->
            validator.validate(
                "http://localhost:3000/payment/success",
                "success_url"
            )
        )
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("local-development");
    }

    @Test
    void localHttpCanBeExplicitlyEnabled() {
        RedirectUrlValidator validator =
            new RedirectUrlValidator(new MockEnvironment(), true);

        assertThat(
            validator.validate(
                "http://localhost:3000/payment/success",
                "success_url"
            )
        ).isEqualTo(
            "http://localhost:3000/payment/success"
        );
    }

    @Test
    void rejectsNonHttpSchemes() {
        RedirectUrlValidator validator =
            new RedirectUrlValidator(new MockEnvironment(), false);

        assertThatThrownBy(() ->
            validator.validate(
                "javascript:alert(1)",
                "success_url"
            )
        )
            .isInstanceOf(ApiException.class);
    }

    private MockEnvironment productionEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");
        return environment;
    }
}