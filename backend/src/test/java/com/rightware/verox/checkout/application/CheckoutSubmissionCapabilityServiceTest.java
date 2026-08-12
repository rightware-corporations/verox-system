package com.rightware.verox.checkout.application;

import com.rightware.verox.authentication.domain.ApiKeyEnvironment;
import com.rightware.verox.checkout.domain.CheckoutSession;
import com.rightware.verox.merchant.domain.Merchant;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CheckoutSubmissionCapabilityServiceTest {

    @Test
    void issuesDeterministicScopedCapabilityAndRejectsWrongValue() {
        CheckoutSubmissionCapabilityService service = new CheckoutSubmissionCapabilityService(
            "test-checkout-capability-secret"
        );
        Merchant merchant = new Merchant("Event Merchant");
        CheckoutSession session = new CheckoutSession(
            "cs_capability123",
            merchant,
            ApiKeyEnvironment.TEST,
            "ORDER-CAPABILITY",
            "Capability test",
            100,
            "MZN",
            "https://merchant.example/success",
            "https://merchant.example/cancel",
            "idem-capability",
            "a".repeat(64),
            Instant.now().plusSeconds(900)
        );

        String first = service.issue(session);
        String second = service.issue(session);

        assertThat(first).startsWith("vx_checkout_");
        assertThat(second).isEqualTo(first);
        assertThat(service.matches(session, first)).isTrue();
        assertThat(service.matches(session, null)).isFalse();
        assertThat(service.matches(session, "vx_checkout_wrong")).isFalse();
    }

    @Test
    void scopesCapabilityToCheckoutIdentity() {
        CheckoutSubmissionCapabilityService service = new CheckoutSubmissionCapabilityService(
            "test-checkout-capability-secret"
        );
        Merchant merchant = new Merchant("Event Merchant");
        CheckoutSession first = session(merchant, "cs_one");
        CheckoutSession second = session(merchant, "cs_two");

        String firstCapability = service.issue(first);

        assertThat(service.matches(first, firstCapability)).isTrue();
        assertThat(service.matches(second, firstCapability)).isFalse();
    }

    private CheckoutSession session(Merchant merchant, String publicId) {
        return new CheckoutSession(
            publicId,
            merchant,
            ApiKeyEnvironment.TEST,
            "ORDER-" + publicId,
            "Capability test",
            100,
            "MZN",
            "https://merchant.example/success",
            "https://merchant.example/cancel",
            "idem-" + publicId,
            "a".repeat(64),
            Instant.now().plusSeconds(900)
        );
    }
}
