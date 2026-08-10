package com.rightware.verox.checkout.domain;

import com.rightware.verox.authentication.domain.ApiKeyEnvironment;
import com.rightware.verox.merchant.domain.Merchant;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CheckoutSessionTest {

    @Test
    void completesOpenCheckoutSessionAtPaymentConfirmationTime() {
        CheckoutSession session = session();
        Instant confirmedAt = Instant.parse("2026-08-10T12:00:00Z");

        session.complete(confirmedAt);

        assertThat(session.getStatus()).isEqualTo(CheckoutSessionStatus.COMPLETED);
        assertThat(session.getCompletedAt()).isEqualTo(confirmedAt);
    }

    @Test
    void completingAlreadyCompletedSessionIsIdempotent() {
        CheckoutSession session = session();
        Instant first = Instant.parse("2026-08-10T12:00:00Z");
        Instant later = Instant.parse("2026-08-10T12:05:00Z");

        session.complete(first);
        session.complete(later);

        assertThat(session.getStatus()).isEqualTo(CheckoutSessionStatus.COMPLETED);
        assertThat(session.getCompletedAt()).isEqualTo(first);
    }

    private CheckoutSession session() {
        Merchant merchant = new Merchant("Event Merchant");
        return new CheckoutSession(
            "cs_completion_test",
            merchant,
            ApiKeyEnvironment.TEST,
            "ORDER-COMPLETE",
            "Completion test",
            100,
            "MZN",
            "https://merchant.example/success",
            "https://merchant.example/cancel",
            "idem-complete",
            "a".repeat(64),
            Instant.now().plusSeconds(600)
        );
    }
}
