package com.rightware.verox.verification.matching;

import com.rightware.verox.evidence.domain.EvidenceOrigin;
import com.rightware.verox.verification.mpesa.ParsedMpesaMessage;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class MpesaEvidenceMatcher {

    public VerificationMatchResult match(
        ParsedMpesaMessage customer,
        ParsedMpesaMessage provider,
        long expectedAmountMinor,
        String expectedCurrency
    ) {
        Objects.requireNonNull(customer, "customer");
        Objects.requireNonNull(provider, "provider");

        if (customer.origin() != EvidenceOrigin.CUSTOMER || provider.origin() != EvidenceOrigin.PROVIDER) {
            return review("EVIDENCE_ORIGIN_MISMATCH");
        }

        if (!customer.isMatchReady() || !provider.isMatchReady()) {
            return review("UNRECOGNIZED_MESSAGE_FORMAT");
        }

        String normalizedCurrency = normalizeCurrency(expectedCurrency);
        if (normalizedCurrency == null) {
            return review("INVALID_EXPECTED_CURRENCY");
        }

        if (!customer.transactionReference().equalsIgnoreCase(provider.transactionReference())) {
            return review("TRANSACTION_REFERENCE_MISMATCH");
        }

        if (!customer.amountMinor().equals(provider.amountMinor())) {
            return review("EVIDENCE_AMOUNT_MISMATCH");
        }

        if (!customer.currency().equalsIgnoreCase(provider.currency())) {
            return review("EVIDENCE_CURRENCY_MISMATCH");
        }

        if (provider.amountMinor() != expectedAmountMinor) {
            return review("PAYMENT_AMOUNT_MISMATCH");
        }

        if (!provider.currency().equalsIgnoreCase(normalizedCurrency)) {
            return review("PAYMENT_CURRENCY_MISMATCH");
        }

        return new VerificationMatchResult(
            VerificationDecision.MATCH,
            "REFERENCE_AMOUNT_AND_CURRENCY_MATCH",
            provider.transactionReference().toUpperCase(),
            provider.amountMinor(),
            provider.currency().toUpperCase()
        );
    }

    private VerificationMatchResult review(String reason) {
        return new VerificationMatchResult(
            VerificationDecision.REVIEW_REQUIRED,
            reason,
            null,
            null,
            null
        );
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return null;
        }
        return currency.trim().toUpperCase();
    }
}
