package com.rightware.verox.verification.matching;

import com.rightware.verox.evidence.domain.EvidenceOrigin;
import com.rightware.verox.verification.mpesa.ParsedMpesaMessage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MpesaEvidenceMatcherTest {

    private final MpesaEvidenceMatcher matcher = new MpesaEvidenceMatcher();

    @Test
    void matchesWhenReferenceAmountCurrencyAndExpectedPaymentAgree() {
        ParsedMpesaMessage customer = message(EvidenceOrigin.CUSTOMER, "DH10L1OJRUS", 100L, "MZN", true);
        ParsedMpesaMessage provider = message(EvidenceOrigin.PROVIDER, "DH10L1OJRUS", 100L, "MZN", true);

        VerificationMatchResult result = matcher.match(customer, provider, 100L, "MZN");

        assertThat(result.decision()).isEqualTo(VerificationDecision.MATCH);
        assertThat(result.reason()).isEqualTo("REFERENCE_AMOUNT_AND_CURRENCY_MATCH");
        assertThat(result.transactionReference()).isEqualTo("DH10L1OJRUS");
        assertThat(result.amountMinor()).isEqualTo(100L);
        assertThat(result.currency()).isEqualTo("MZN");
    }

    @Test
    void requiresReviewWhenTransactionReferenceDiffers() {
        ParsedMpesaMessage customer = message(EvidenceOrigin.CUSTOMER, "DH10L1OJRUS", 100L, "MZN", true);
        ParsedMpesaMessage provider = message(EvidenceOrigin.PROVIDER, "ZZ99L1OJRUS", 100L, "MZN", true);

        VerificationMatchResult result = matcher.match(customer, provider, 100L, "MZN");

        assertReview(result, "TRANSACTION_REFERENCE_MISMATCH");
    }

    @Test
    void requiresReviewWhenEvidenceAmountsDiffer() {
        ParsedMpesaMessage customer = message(EvidenceOrigin.CUSTOMER, "DH10L1OJRUS", 100L, "MZN", true);
        ParsedMpesaMessage provider = message(EvidenceOrigin.PROVIDER, "DH10L1OJRUS", 200L, "MZN", true);

        VerificationMatchResult result = matcher.match(customer, provider, 100L, "MZN");

        assertReview(result, "EVIDENCE_AMOUNT_MISMATCH");
    }

    @Test
    void requiresReviewWhenEvidenceMatchesButPaymentAmountDiffers() {
        ParsedMpesaMessage customer = message(EvidenceOrigin.CUSTOMER, "DH10L1OJRUS", 100L, "MZN", true);
        ParsedMpesaMessage provider = message(EvidenceOrigin.PROVIDER, "DH10L1OJRUS", 100L, "MZN", true);

        VerificationMatchResult result = matcher.match(customer, provider, 150000L, "MZN");

        assertReview(result, "PAYMENT_AMOUNT_MISMATCH");
    }

    @Test
    void requiresReviewWhenOneMessageIsNotRecognized() {
        ParsedMpesaMessage customer = message(EvidenceOrigin.CUSTOMER, null, null, null, false);
        ParsedMpesaMessage provider = message(EvidenceOrigin.PROVIDER, "DH10L1OJRUS", 100L, "MZN", true);

        VerificationMatchResult result = matcher.match(customer, provider, 100L, "MZN");

        assertReview(result, "UNRECOGNIZED_MESSAGE_FORMAT");
    }

    @Test
    void requiresReviewWhenOriginsAreNotCustomerAndProvider() {
        ParsedMpesaMessage customer = message(EvidenceOrigin.PROVIDER, "DH10L1OJRUS", 100L, "MZN", true);
        ParsedMpesaMessage provider = message(EvidenceOrigin.PROVIDER, "DH10L1OJRUS", 100L, "MZN", true);

        VerificationMatchResult result = matcher.match(customer, provider, 100L, "MZN");

        assertReview(result, "EVIDENCE_ORIGIN_MISMATCH");
    }

    @Test
    void requiresReviewWhenExpectedCurrencyDiffers() {
        ParsedMpesaMessage customer = message(EvidenceOrigin.CUSTOMER, "DH10L1OJRUS", 100L, "MZN", true);
        ParsedMpesaMessage provider = message(EvidenceOrigin.PROVIDER, "DH10L1OJRUS", 100L, "MZN", true);

        VerificationMatchResult result = matcher.match(customer, provider, 100L, "USD");

        assertReview(result, "PAYMENT_CURRENCY_MISMATCH");
    }

    private ParsedMpesaMessage message(
        EvidenceOrigin origin,
        String reference,
        Long amountMinor,
        String currency,
        boolean recognized
    ) {
        return new ParsedMpesaMessage(origin, reference, amountMinor, currency, recognized);
    }

    private void assertReview(VerificationMatchResult result, String reason) {
        assertThat(result.decision()).isEqualTo(VerificationDecision.REVIEW_REQUIRED);
        assertThat(result.reason()).isEqualTo(reason);
        assertThat(result.transactionReference()).isNull();
        assertThat(result.amountMinor()).isNull();
        assertThat(result.currency()).isNull();
    }
}
