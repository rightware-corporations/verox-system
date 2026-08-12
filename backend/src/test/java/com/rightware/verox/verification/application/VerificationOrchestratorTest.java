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
import com.rightware.verox.payment.application.PaymentConfirmedEvent;
import com.rightware.verox.payment.domain.Payment;
import com.rightware.verox.payment.domain.PaymentStatus;
import com.rightware.verox.payment.repository.PaymentRepository;
import com.rightware.verox.verification.matching.MpesaEvidenceMatcher;
import com.rightware.verox.verification.mpesa.MpesaMessageParser;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VerificationOrchestratorTest {

    private static final long PROVIDER_EVIDENCE_GRACE_SECONDS = 300;

    @Test
    void confirmsOnlyOneDeterministicCustomerProviderPair() {
        Fixture fixture = fixture();
        Evidence customer = customerEvidence(fixture.merchant(), fixture.payment(), "DH10L1OJRUS", "1.00");
        Evidence provider = providerEvidence(fixture.merchant(), ApiKeyEnvironment.TEST, "DH10L1OJRUS", "1.00", "ev_provider_match");
        stubPaymentEvidence(fixture, List.of(customer));
        stubReferenceUsed(fixture, "DH10L1OJRUS", false);
        stubEligibleProviderEvidence(fixture, List.of(provider));

        VerificationRunResult result = fixture.orchestrator().verifyPayment(fixture.payment().getId());

        assertThat(result.status()).isEqualTo(VerificationRunStatus.CONFIRMED);
        assertThat(result.providerEvidenceId()).isEqualTo("ev_provider_match");
        assertThat(fixture.payment().getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
        verify(fixture.evidenceService()).linkProviderEvidence(provider, fixture.payment());
        verify(fixture.paymentRepository()).save(fixture.payment());

        ArgumentCaptor<PaymentConfirmedEvent> captor = ArgumentCaptor.forClass(PaymentConfirmedEvent.class);
        verify(fixture.eventPublisher()).publishEvent(captor.capture());
        PaymentConfirmedEvent event = captor.getValue();
        assertThat(event.paymentId()).isEqualTo("pay_verify_test");
        assertThat(event.checkoutSessionId()).isEqualTo("cs_verify_test");
        assertThat(event.externalReference()).isEqualTo("ORDER-VERIFY");
        assertThat(event.amount()).isEqualTo("1.00");
        assertThat(event.currency()).isEqualTo("MZN");
        assertThat(event.provider()).isEqualTo("MPESA");
        assertThat(event.providerTransactionReference()).isEqualTo("DH10L1OJRUS");
    }

    @Test
    void ignoresUnrelatedProviderSmsAndKeepsWaiting() {
        Fixture fixture = fixture();
        Evidence customer = customerEvidence(fixture.merchant(), fixture.payment(), "DH10L1OJRUS", "1.00");
        Evidence unrelatedProvider = providerEvidence(fixture.merchant(), ApiKeyEnvironment.TEST, "ZZ99YY88XX77", "1.00", "ev_unrelated");
        stubPaymentEvidence(fixture, List.of(customer));
        stubReferenceUsed(fixture, "DH10L1OJRUS", false);
        stubEligibleProviderEvidence(fixture, List.of(unrelatedProvider));

        VerificationRunResult result = fixture.orchestrator().verifyPayment(fixture.payment().getId());

        assertThat(result.status()).isEqualTo(VerificationRunStatus.WAITING_PROVIDER);
        assertThat(fixture.payment().getStatus()).isEqualTo(PaymentStatus.VERIFYING);
        verify(fixture.evidenceService(), never()).linkProviderEvidence(any(), any());
        verify(fixture.eventPublisher(), never()).publishEvent(any(PaymentConfirmedEvent.class));
    }

    @Test
    void sendsSameReferenceAmountConflictToReview() {
        Fixture fixture = fixture();
        Evidence customer = customerEvidence(fixture.merchant(), fixture.payment(), "DH10L1OJRUS", "1.00");
        Evidence provider = providerEvidence(fixture.merchant(), ApiKeyEnvironment.TEST, "DH10L1OJRUS", "2.00", "ev_amount_conflict");
        stubPaymentEvidence(fixture, List.of(customer));
        stubReferenceUsed(fixture, "DH10L1OJRUS", false);
        stubEligibleProviderEvidence(fixture, List.of(provider));

        VerificationRunResult result = fixture.orchestrator().verifyPayment(fixture.payment().getId());

        assertThat(result.status()).isEqualTo(VerificationRunStatus.REVIEW_REQUIRED);
        assertThat(result.reason()).isEqualTo("EVIDENCE_AMOUNT_MISMATCH");
        assertThat(fixture.payment().getStatus()).isEqualTo(PaymentStatus.REVIEW_REQUIRED);
        verify(fixture.evidenceService(), never()).linkProviderEvidence(any(), any());
        verify(fixture.eventPublisher(), never()).publishEvent(any(PaymentConfirmedEvent.class));
    }

    @Test
    void multipleProviderMessagesWithSameReferenceAreAmbiguous() {
        Fixture fixture = fixture();
        Evidence customer = customerEvidence(fixture.merchant(), fixture.payment(), "DH10L1OJRUS", "1.00");
        Evidence providerOne = providerEvidence(fixture.merchant(), ApiKeyEnvironment.TEST, "DH10L1OJRUS", "1.00", "ev_provider_one");
        Evidence providerTwo = providerEvidence(fixture.merchant(), ApiKeyEnvironment.TEST, "DH10L1OJRUS", "1.00", "ev_provider_two");
        stubPaymentEvidence(fixture, List.of(customer));
        stubReferenceUsed(fixture, "DH10L1OJRUS", false);
        stubEligibleProviderEvidence(fixture, List.of(providerOne, providerTwo));

        VerificationRunResult result = fixture.orchestrator().verifyPayment(fixture.payment().getId());

        assertThat(result.status()).isEqualTo(VerificationRunStatus.REVIEW_REQUIRED);
        assertThat(result.reason()).isEqualTo("PROVIDER_EVIDENCE_AMBIGUOUS");
        assertThat(fixture.payment().getStatus()).isEqualTo(PaymentStatus.REVIEW_REQUIRED);
        verify(fixture.eventPublisher(), never()).publishEvent(any(PaymentConfirmedEvent.class));
    }

    @Test
    void reusedCustomerReferenceCannotPoisonPaymentOrConfirmAgain() {
        Fixture fixture = fixture();
        Evidence customer = customerEvidence(fixture.merchant(), fixture.payment(), "DH10L1OJRUS", "1.00");
        stubPaymentEvidence(fixture, List.of(customer));
        stubReferenceUsed(fixture, "DH10L1OJRUS", true);

        VerificationRunResult result = fixture.orchestrator().verifyPayment(fixture.payment().getId());

        assertThat(result.status()).isEqualTo(VerificationRunStatus.WAITING_CUSTOMER);
        assertThat(result.reason()).isEqualTo("CUSTOMER_TRANSACTION_REFERENCE_ALREADY_USED");
        assertThat(fixture.payment().getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(fixture.evidenceRepository(), never())
            .findAllByMerchantIdAndEnvironmentAndOriginAndProviderIgnoreCaseAndPaymentIsNullAndCreatedAtBetweenOrderByCreatedAtAsc(
                any(), any(), any(), any(), any(), any()
            );
        verify(fixture.evidenceService(), never()).linkProviderEvidence(any(), any());
        verify(fixture.eventPublisher(), never()).publishEvent(any(PaymentConfirmedEvent.class));
    }

    @Test
    void unrelatedCustomerClaimDoesNotPoisonUniqueRealPair() {
        Fixture fixture = fixture();
        Evidence legitimate = customerEvidence(fixture.merchant(), fixture.payment(), "DH10L1OJRUS", "1.00");
        Evidence unrelated = customerEvidence(fixture.merchant(), fixture.payment(), "AB12CD34EF56", "1.00");
        Evidence provider = providerEvidence(fixture.merchant(), ApiKeyEnvironment.TEST, "DH10L1OJRUS", "1.00", "ev_provider_match");
        stubPaymentEvidence(fixture, List.of(unrelated, legitimate));
        stubReferenceUsed(fixture, "DH10L1OJRUS", false);
        stubReferenceUsed(fixture, "AB12CD34EF56", false);
        stubEligibleProviderEvidence(fixture, List.of(provider));

        VerificationRunResult result = fixture.orchestrator().verifyPayment(fixture.payment().getId());

        assertThat(result.status()).isEqualTo(VerificationRunStatus.CONFIRMED);
        assertThat(result.transactionReference()).isEqualTo("DH10L1OJRUS");
        assertThat(fixture.payment().getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
        verify(fixture.evidenceService()).linkProviderEvidence(provider, fixture.payment());
    }

    @Test
    void multipleRealEvidencePairsStillFailClosedToReview() {
        Fixture fixture = fixture();
        Evidence customerOne = customerEvidence(fixture.merchant(), fixture.payment(), "DH10L1OJRUS", "1.00");
        Evidence customerTwo = customerEvidence(fixture.merchant(), fixture.payment(), "AB12CD34EF56", "1.00");
        Evidence providerOne = providerEvidence(fixture.merchant(), ApiKeyEnvironment.TEST, "DH10L1OJRUS", "1.00", "ev_provider_one");
        Evidence providerTwo = providerEvidence(fixture.merchant(), ApiKeyEnvironment.TEST, "AB12CD34EF56", "1.00", "ev_provider_two");
        stubPaymentEvidence(fixture, List.of(customerOne, customerTwo));
        stubReferenceUsed(fixture, "DH10L1OJRUS", false);
        stubReferenceUsed(fixture, "AB12CD34EF56", false);
        stubEligibleProviderEvidence(fixture, List.of(providerOne, providerTwo));

        VerificationRunResult result = fixture.orchestrator().verifyPayment(fixture.payment().getId());

        assertThat(result.status()).isEqualTo(VerificationRunStatus.REVIEW_REQUIRED);
        assertThat(result.reason()).isEqualTo("MULTIPLE_MATCHED_EVIDENCE_PAIRS");
        assertThat(fixture.payment().getStatus()).isEqualTo(PaymentStatus.REVIEW_REQUIRED);
        verify(fixture.evidenceService(), never()).linkProviderEvidence(any(), any());
        verify(fixture.eventPublisher(), never()).publishEvent(any(PaymentConfirmedEvent.class));
    }

    @Test
    void unrecognizedCustomerEvidenceDoesNotForceReview() {
        Fixture fixture = fixture();
        Evidence garbage = rawCustomerEvidence(
            fixture.merchant(),
            fixture.payment(),
            "ev_customer_garbage",
            "This is not a recognized payment confirmation message."
        );
        stubPaymentEvidence(fixture, List.of(garbage));

        VerificationRunResult result = fixture.orchestrator().verifyPayment(fixture.payment().getId());

        assertThat(result.status()).isEqualTo(VerificationRunStatus.WAITING_CUSTOMER);
        assertThat(result.reason()).isEqualTo("CUSTOMER_EVIDENCE_NOT_MATCH_READY");
        assertThat(fixture.payment().getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(fixture.evidenceService(), never()).linkProviderEvidence(any(), any());
        verify(fixture.eventPublisher(), never()).publishEvent(any(PaymentConfirmedEvent.class));
    }

    @Test
    void wrongAmountCustomerEvidenceDoesNotForceReview() {
        Fixture fixture = fixture();
        Evidence wrongAmount = customerEvidence(fixture.merchant(), fixture.payment(), "DH10L1OJRUS", "2.00");
        stubPaymentEvidence(fixture, List.of(wrongAmount));

        VerificationRunResult result = fixture.orchestrator().verifyPayment(fixture.payment().getId());

        assertThat(result.status()).isEqualTo(VerificationRunStatus.WAITING_CUSTOMER);
        assertThat(result.reason()).isEqualTo("CUSTOMER_EVIDENCE_NOT_MATCH_READY");
        assertThat(fixture.payment().getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(fixture.evidenceService(), never()).linkProviderEvidence(any(), any());
        verify(fixture.eventPublisher(), never()).publishEvent(any(PaymentConfirmedEvent.class));
    }

    @Test
    void providerLookupUsesPaymentEnvironmentAndServerObservedCheckoutWindow() {
        Fixture fixture = fixture();
        Evidence customer = customerEvidence(fixture.merchant(), fixture.payment(), "DH10L1OJRUS", "1.00");
        stubPaymentEvidence(fixture, List.of(customer));
        stubReferenceUsed(fixture, "DH10L1OJRUS", false);

        VerificationRunResult result = fixture.orchestrator().verifyPayment(fixture.payment().getId());

        assertThat(result.status()).isEqualTo(VerificationRunStatus.WAITING_PROVIDER);
        verify(fixture.evidenceRepository())
            .findAllByMerchantIdAndEnvironmentAndOriginAndProviderIgnoreCaseAndPaymentIsNullAndCreatedAtBetweenOrderByCreatedAtAsc(
                fixture.merchant().getId(),
                ApiKeyEnvironment.TEST,
                EvidenceOrigin.PROVIDER,
                "MPESA",
                fixture.payment().getCheckoutSession().getCreatedAt(),
                fixture.payment().getCheckoutSession().getExpiresAt().plusSeconds(PROVIDER_EVIDENCE_GRACE_SECONDS)
            );
    }

    @Test
    void merchantVerificationScopesActivePaymentsToBridgeEnvironment() {
        Fixture fixture = fixture();
        when(fixture.paymentRepository().findAllByMerchantIdAndEnvironmentAndStatusInOrderByCreatedAtAsc(
            eq(fixture.merchant().getId()),
            eq(ApiKeyEnvironment.LIVE),
            anyCollection()
        )).thenReturn(List.of());

        List<VerificationRunResult> results = fixture.orchestrator().verifyMerchant(
            fixture.merchant().getId(),
            ApiKeyEnvironment.LIVE
        );

        assertThat(results).isEmpty();
        verify(fixture.paymentRepository()).findAllByMerchantIdAndEnvironmentAndStatusInOrderByCreatedAtAsc(
            eq(fixture.merchant().getId()),
            eq(ApiKeyEnvironment.LIVE),
            anyCollection()
        );
    }

    private void stubPaymentEvidence(Fixture fixture, List<Evidence> evidence) {
        when(fixture.paymentRepository().findById(fixture.payment().getId())).thenReturn(Optional.of(fixture.payment()));
        when(fixture.evidenceRepository().findAllByPaymentIdOrderByReceivedAtAsc(fixture.payment().getId())).thenReturn(evidence);
    }

    private void stubReferenceUsed(Fixture fixture, String reference, boolean used) {
        when(fixture.paymentRepository().existsByMerchantIdAndEnvironmentAndProviderIgnoreCaseAndProviderTransactionReferenceIgnoreCaseAndIdNot(
            fixture.merchant().getId(),
            fixture.payment().getEnvironment(),
            "MPESA",
            reference,
            fixture.payment().getId()
        )).thenReturn(used);
    }

    private void stubEligibleProviderEvidence(Fixture fixture, List<Evidence> evidence) {
        when(fixture.evidenceRepository()
            .findAllByMerchantIdAndEnvironmentAndOriginAndProviderIgnoreCaseAndPaymentIsNullAndCreatedAtBetweenOrderByCreatedAtAsc(
                fixture.merchant().getId(),
                fixture.payment().getEnvironment(),
                EvidenceOrigin.PROVIDER,
                "MPESA",
                fixture.payment().getCheckoutSession().getCreatedAt(),
                fixture.payment().getCheckoutSession().getExpiresAt().plusSeconds(PROVIDER_EVIDENCE_GRACE_SECONDS)
            )).thenReturn(evidence);
    }

    private Fixture fixture() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        EvidenceRepository evidenceRepository = mock(EvidenceRepository.class);
        EvidenceService evidenceService = mock(EvidenceService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        Merchant merchant = new Merchant("Event Merchant");
        CheckoutSession checkoutSession = new CheckoutSession(
            "cs_verify_test", merchant, ApiKeyEnvironment.TEST, "ORDER-VERIFY", "Verification test",
            100, "MZN", "https://merchant.example/success", "https://merchant.example/cancel",
            "idem-verify", "a".repeat(64), Instant.now().plusSeconds(600)
        );
        Payment payment = new Payment("pay_verify_test", merchant, checkoutSession, ApiKeyEnvironment.TEST, 100, "MZN");

        VerificationOrchestrator orchestrator = new VerificationOrchestrator(
            paymentRepository,
            evidenceRepository,
            evidenceService,
            new MpesaMessageParser(),
            new MpesaEvidenceMatcher(),
            eventPublisher,
            PROVIDER_EVIDENCE_GRACE_SECONDS
        );

        return new Fixture(paymentRepository, evidenceRepository, evidenceService, eventPublisher, merchant, payment, orchestrator);
    }

    private Evidence customerEvidence(Merchant merchant, Payment payment, String reference, String amount) {
        return rawCustomerEvidence(
            merchant,
            payment,
            "ev_customer_" + reference,
            "Confirmado " + reference + ". Transferiste " + amount + "MT via M-Pesa."
        );
    }

    private Evidence rawCustomerEvidence(
        Merchant merchant,
        Payment payment,
        String publicId,
        String rawContent
    ) {
        return new Evidence(
            publicId,
            merchant,
            payment,
            payment.getEnvironment(),
            EvidenceOrigin.CUSTOMER,
            EvidenceKind.SMS,
            EvidenceIngestSource.HOSTED_CHECKOUT,
            "MPESA",
            hash(publicId + rawContent),
            "text/plain",
            null,
            null,
            rawContent,
            null,
            Instant.now()
        );
    }

    private Evidence providerEvidence(
        Merchant merchant,
        ApiKeyEnvironment environment,
        String reference,
        String amount,
        String publicId
    ) {
        return new Evidence(
            publicId,
            merchant,
            null,
            environment,
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
        ApplicationEventPublisher eventPublisher,
        Merchant merchant,
        Payment payment,
        VerificationOrchestrator orchestrator
    ) {
    }
}
