package com.rightware.verox.verification.application;

import com.rightware.verox.evidence.application.EvidenceIngestedEvent;
import com.rightware.verox.evidence.domain.EvidenceOrigin;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class VerificationEvidenceIngestedListenerTest {

    @Test
    void customerEvidenceTriggersOnlyItsPaymentVerification() {
        VerificationOrchestrator orchestrator = mock(VerificationOrchestrator.class);
        VerificationEvidenceIngestedListener listener = new VerificationEvidenceIngestedListener(orchestrator);
        UUID merchantId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        when(orchestrator.verifyPayment(paymentId)).thenReturn(new VerificationRunResult(
            "pay_test123",
            VerificationRunStatus.WAITING_PROVIDER,
            "MATCHING_PROVIDER_EVIDENCE_NOT_AVAILABLE",
            null,
            "DH10L1OJRUS"
        ));

        listener.onEvidenceIngested(new EvidenceIngestedEvent(
            merchantId,
            paymentId,
            EvidenceOrigin.CUSTOMER,
            "ev_customer123"
        ));

        verify(orchestrator).verifyPayment(paymentId);
        verifyNoMoreInteractions(orchestrator);
    }

    @Test
    void providerEvidenceTriggersMerchantVerificationBecauseItStartsUnlinked() {
        VerificationOrchestrator orchestrator = mock(VerificationOrchestrator.class);
        VerificationEvidenceIngestedListener listener = new VerificationEvidenceIngestedListener(orchestrator);
        UUID merchantId = UUID.randomUUID();

        when(orchestrator.verifyMerchant(merchantId)).thenReturn(List.of());

        listener.onEvidenceIngested(new EvidenceIngestedEvent(
            merchantId,
            null,
            EvidenceOrigin.PROVIDER,
            "ev_provider123"
        ));

        verify(orchestrator).verifyMerchant(merchantId);
        verifyNoMoreInteractions(orchestrator);
    }

    @Test
    void verificationFailureDoesNotEscapeAfterEvidenceWasCommitted() {
        VerificationOrchestrator orchestrator = mock(VerificationOrchestrator.class);
        VerificationEvidenceIngestedListener listener = new VerificationEvidenceIngestedListener(orchestrator);
        UUID merchantId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        doThrow(new IllegalStateException("verification failure"))
            .when(orchestrator).verifyPayment(paymentId);

        assertThatCode(() -> listener.onEvidenceIngested(new EvidenceIngestedEvent(
            merchantId,
            paymentId,
            EvidenceOrigin.CUSTOMER,
            "ev_customer123"
        ))).doesNotThrowAnyException();

        verify(orchestrator).verifyPayment(paymentId);
    }
}
