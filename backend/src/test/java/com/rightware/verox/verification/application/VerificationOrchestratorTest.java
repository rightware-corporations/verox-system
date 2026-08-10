package com.rightware.verox.verification.application;

import com.rightware.verox.authentication.domain.ApiKeyEnvironment;
import com.rightware.verox.checkout.domain.CheckoutSession;
import com.rightware.verox.evidence.application.EvidenceService;
import com.rightware.verox.evidence.domain.Evidence;
import com.rightware.verox.evidence.domain.EvidenceIngestSource;
import com.rightware.verox.evidence.domain.EvidenceKind;
import com.rightware.verox.evidence.domain.EvidenceOrigin;
import com.rightware.verox.evidence.repository.EvidenceRepository;
import com.rightware.verox.merchant.domain.Merchant;
import com.rightware.verox.payment.domain.Payment;
import com.rightware.verox.payment.domain.PaymentStatus;
import com.rightware.verox.payment.repository.PaymentRepository;
import com.rightware.verox.verification.matching.MpesaEvidenceMatcher;
import com.rightware.verox.verification.mpesa.MpesaMessageParser;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VerificationOrchestratorTest {

    @Test
    void confirmsOnlyOneDeterministicCustomerProviderPair() {
        Fixture fixture = fixture();
        Evidence customer = customerEvidence(fixture.merchant(), fixture.payment(), "DH10L1OJRUS", "1.00");
        Evidence provider = providerEvidence(fixture.merchant(), "DH10L1OJRUS", "1.00", "ev_provider_match");

        when(fixture.paymentRepository().findById(fixture.payment().getId()))
            .thenReturn(Optional.of(fixture.payment()));
        when(fixture.evidenceRepository().findAllByPaymentIdOrderByReceivedAtAsc(fixture.payment().getId()))
            .thenReturn(List.of(customer));
        when(fixture.paymentRepository().existsByMerchantIdAndProviderIgnoreCaseAndProviderTransactionReferenceIgnoreCaseAndIdNot(
            fixture.merchant().getId(), "MPESA", "DH10L1OJRUS", fixture.payment().getId()
        )).thenReturn(false);
        when(fixture.evidenceRepository().findAllByMerchantIdAndOriginAndPaymentIsNullOrderByReceivedAtAsc(
            fixture.merchant().getId(), EvidenceOrigin.PROVIDER
        )).thenReturn(List.of(provider));

        VerificationRunResult result = fixture.orchestrator().verifyPayment(fixture.payment().getId());

        assertThat(result.status()).isEqualTo(VerificationRunStatus.CONFIRMED);
        assertThat(result.providerEvidenceId()).isEqualTo("ev_provider_match");
        assertThat(result.transactionReference()).isEqualTo("DH10L1OJRUS");
        assertThat(fixture.payment().getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(fixture.payment().getProvider()).isEqualTo("MPESA");
        assertThat(fixture.payment().getProviderTransactionReference()).isEqualTo("DH10L1OJRUS");
        verify(fixture.evidenceService()).linkProviderEvidence(provider, fixture.payment());
        verify(fixture.paymentRepository()).save(fixture.payment());
    }

    @Test
    void ignoresUnrelatedProviderSmsAndKeepsWaiting() {
        Fixture fixture = fixture();
        Evidence customer = customerEvidence(fixture.merchant(), fixture.payment(), "DH10L1OJRUS", "1.00");
        Evidence unrelatedProvider = providerEvidence(fixture.merchant(), "ZZ99YY88XX77", "1.00", "ev_unrelated");
        stubActivePayment(fixture, customer);
        when(fixture.evidenceRepository().findAllByMerchantIdAndOriginAndPaymentIsNullOrderByReceivedAtAsc(
            fixture.merchant().getId(), EvidenceOrigin.PROVIDER
        )).thenReturn(List.of(unrelatedProvider));

        VerificationRunResult result = fixture.orchestrator().verifyPayment(fixture.payment().getId());

        assertThat(result.status()).isEqualTo(VerificationRunStatus.WAITING_PROVIDER);
        assertThat(fixture.payment().getStatus()).isEqualTo(PaymentStatus.VERIFYING);
        verify(fixture.evidenceService(), never()).linkProviderEvidence(any(), any());
    }

    @Test
    void sendsSameReferenceAmountConflictToReview() {
        Fixture fixture = fixture();
        Evidence customer = customerEvidence(fixture.merchant(), fixture.payment(), "DH10L1OJRUS", "1.00");
        Evidence provider = providerEvidence(fixture.merchant(), "DH10L1OJRUS", "2.00", "ev_amount_conflict");
        stubActivePayment(fixture, customer);
        when(fixture.evidenceRepository().findAllByMerchantIdAndOriginAndPaymentIsNullOrderByReceivedAtAsc(
            fixture.merchant().getId(), EvidenceOrigin.PROVIDER
        )).thenReturn(List.of(provider));

        VerificationRunResult result = fixture.orchestrator().verifyPayment(fixture.payment().getId());

        assertThat(result.status()).isEqualTo(VerificationRunStatus.REVIEW_REQUIRED);
        assertThat(result.reason()).isEqualTo("EVIDENCE_AMOUNT_MISMATCH");
        assertThat(fixture.payment().getStatus()).isEqualTo(PaymentStatus.REVIEW_REQUIRED);
        verify(fixture.evidenceService(), never()).linkProviderEvidence(any(), any());
    }

    @Test
    void multipleProviderMessagesWithSameReferenceAreAmbiguous() {
        Fixture fixture = fixture();
        Evidence customer = customerEvidence(fixture.merchant(), fixture.payment(), "DH10L1OJRUS", "1.00");
        Evidence providerOne = providerEvidence(fixture.merchant(), "DH10L1OJRUS", "1.00", "ev_provider_one");
        Evidence providerTwo = providerEvidence(fixture.merchant(), "DH10L1OJRUS", "1.00", "ev_provider_two");
        stubActivePayment(fixture, customer);
        when(fixture.evidenceRepository().findAllByMerchantIdAndOriginAndPaymentIsNullOrderByReceivedAtAsc(
            fixture.merchant().getId(), EvidenceOrigin.PROVIDER
        )).thenReturn(List.of(providerOne, providerTwo));

        VerificationRunResult result = fixture.orchestrator().verifyPayment(fixture.payment().getId());

        assertThat(result.status()).isEqualTo(VerificationRunStatus.REVIEW_REQUIRED);
        assertThat(result.reason()).isEqualTo("PROVIDER_EVIDENCE_AMBIGUOUS");
        assertThat(fixture.payment().getStatus()).isEqualTo(PaymentStatus.REVIEW_REQUIRED);
        verify(fixture.evidenceService(), never()).linkProviderEvidence(any(), any());
    }

    @Test
    void reusedProviderTransactionReferenceCannotConfirmAnotherPayment() {
        Fixture fixture = fixture();
        Evidence customer = customerEvidence(fixture.merchant(), fixture.payment(), "DH10L1OJRUS", "1.00");

        when(fixture.paymentRepository().findById(fixture.payment().getId()))
            .thenReturn(Optional.of(fixture.payment()));
        when(fixture.evidenceRepository().findAllByPaymentIdOrderByReceivedAtAsc(fixture.payment().getId()))
            .thenReturn(List.of(customer));
        when(fixture.paymentRepository().existsByMerchantIdAndProviderIgnoreCaseAndProviderTransactionReferenceIgnoreCaseAndIdNot(
            fixture.merchant().getId(), "MPESA", "DH10L1OJRUS", fixture.payment().getId()
        )).thenReturn(true);

        VerificationRunResult result = fixture.orchestrator().verifyPayment(fixture.payment().getId());

        assertThat(result.status()).isEqualTo(VerificationRunStatus.REVIEW_REQUIRED);
        assertThat(result.reason()).isEqualTo("TRANSACTION_REFERENCE_ALREADY_USED");
        assertThat(fixture.payment().getStatus()).isEqualTo(PaymentStatus.REVIEW_REQUIRED);
        verify(fixture.evidenceRepository(), never())
            .findAllByMerchantIdAndOriginAndPaymentIsNullOrderByReceivedAtAsc(any(), any());
    }

    @Test
    void conflictingCustomerMessagesAreAmbiguous() {
        Fixture fixture = fixture();
        Evidence first = customerEvidence(fixture.merchant(), fixture.payment(), "DH10L1OJRUS", "1.00");
        Evidence second = customerEvidence(fixture.merchant(), fixture.payment(), "AB12CD34EF56", "1.00");

        when(fixture.paymentRepository().findById(fixture.payment().getId()))
            .thenReturn(Optional.of(fixture.payment()));
        when(fixture.evidenceRepository().findAllByPaymentIdOrderByReceivedAtAsc(fixture.payment().getId()))
            .thenReturn(List.of(first, second));

        VerificationRunResult result = fixture.orchestrator().verifyPayment(fixture.payment().getId());

        assertThat(result.status()).isEqualTo(VerificationRunStatus.REVIEW_REQUIRED);
        assertThat(result.reason()).isEqualTo("CUSTOMER_EVIDENCE_AMBIGUOUS");
        assertThat(fixture.payment().getStatus()).isEqualTo(PaymentStatus.REVIEW_REQUIRED);
    }

    private void stubActivePayment(Fixture fixture, Evidence customer) {
        when(fixture.paymentRepository().findById(fixture.payment().getId()))
            .thenReturn(Optional.of(fixture.payment()));
        when(fixture.evidenceRepository().findAllByPaymentIdOrderByReceivedAtAsc(fixture.payment().getId()))
            .thenReturn(List.of(customer));
        when(fixture.paymentRepository().existsByMerchantIdAndProviderIgnoreCaseAndProviderTransactionReferenceIgnoreCaseAndIdNot(
            fixture.merchant().getId(), "MPESA", "DH10L1OJRUS", fixture.payment().getId()
        )).thenReturn(false);
    }

    private Fixture fixture() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        EvidenceRepository evidenceRepository = mock(EvidenceRepository.class);
        EvidenceService evidenceService = mock(EvidenceService.class);
        Merchant merchant = new Merchant("Event Merchant");
        CheckoutSession checkoutSession = new CheckoutSession(
            "cs_verify_test",
            merchant,
            ApiKeyEnvironment.TEST,
            "ORDER-VERIFY",
            "Verification test",
            100,
            "MZN",
            "https://merchant.example/success",
            "https://merchant.example/cancel",
            "idem-verify",
            "a".repeat(64),
            Instant.now().plusSeconds(600)
        );
        Payment payment = new Payment(
            "pay_verify_test",
            merchant,
            checkoutSession,
            ApiKeyEnvironment.TEST,
            100,
            "MZN"
        );

        VerificationOrchestrator orchestrator = new VerificationOrchestrator(
            paymentRepository,
            evidenceRepository,
            evidenceService,
            new MpesaMessageParser(),
            new MpesaEvidenceMatcher()
        );

        return new Fixture(
            paymentRepository,
            evidenceRepository,
            evidenceService,
            merchant,
            payment,
            orchestrator
        );
    }

    private Evidence customerEvidence(Merchant merchant, Payment payment, String reference, String amount) {
        return new Evidence(
            "ev_customer_" + reference,
            merchant,
            payment,
            EvidenceOrigin.CUSTOMER,
            EvidenceKind.SMS,
            EvidenceIngestSource.HOSTED_CHECKOUT,
            "MPESA",
            hash(reference + amount + "customer"),
            "text/plain",
            null,
            null,
            "Confirmado " + reference + ". Transferiste " + amount + "MT via M-Pesa.",
            null,
            Instant.now()
        );
    }

    private Evidence providerEvidence(Merchant merchant, String reference, String amount, String publicId) {
        return new Evidence(
            publicId,
            merchant,
            null,
            EvidenceOrigin.PROVIDER,
            EvidenceKind.SMS,
            EvidenceIngestSource.VEROX_BRIDGE,
            "MPESA",
            hash(reference + amount + publicId),
            "text/plain",
            null,
            null,
            reference + " Confirmed.You have received " + amount + "MT via M-Pesa.",
            null,
            Instant.now()
        );
    }

    private String hash(String seed) {
        String value = Integer.toHexString(seed.hashCode()).replace("-", "0");
        return (value + "0".repeat(64)).substring(0, 64);
    }

    private record Fixture(
        PaymentRepository paymentRepository,
        EvidenceRepository evidenceRepository,
        EvidenceService evidenceService,
        Merchant merchant,
        Payment payment,
        VerificationOrchestrator orchestrator
    ) {
    }
}
