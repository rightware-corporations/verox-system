package com.rightware.verox.evidence.application;

import com.rightware.verox.authentication.domain.ApiKeyEnvironment;
import com.rightware.verox.checkout.domain.CheckoutSession;
import com.rightware.verox.checkout.repository.CheckoutSessionRepository;
import com.rightware.verox.common.web.ApiException;
import com.rightware.verox.evidence.domain.Evidence;
import com.rightware.verox.evidence.domain.EvidenceIngestSource;
import com.rightware.verox.evidence.domain.EvidenceKind;
import com.rightware.verox.evidence.domain.EvidenceOrigin;
import com.rightware.verox.merchant.domain.Merchant;
import com.rightware.verox.payment.domain.Payment;
import com.rightware.verox.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerMessageEvidenceIngestionServiceTest {

    @Test
    void ingestsPastedCustomerMessageWithoutManualPaymentFieldsAndPublishesVerificationEvent() {
        CheckoutSessionRepository checkoutSessionRepository = mock(CheckoutSessionRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        EvidenceService evidenceService = mock(EvidenceService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        Merchant merchant = new Merchant("Event Merchant");
        CheckoutSession session = session(merchant, Instant.now().plusSeconds(600));
        Payment payment = new Payment(
            "pay_test123",
            merchant,
            session,
            ApiKeyEnvironment.TEST,
            100,
            "MZN"
        );
        Instant receivedAt = Instant.parse("2026-08-09T16:00:00Z");
        Evidence evidence = new Evidence(
            "ev_customer123",
            merchant,
            payment,
            EvidenceOrigin.CUSTOMER,
            EvidenceKind.SMS,
            EvidenceIngestSource.HOSTED_CHECKOUT,
            "MPESA",
            "b".repeat(64),
            "text/plain",
            null,
            null,
            "Confirmado ABC123. Transferiste 1.00MT.",
            null,
            receivedAt
        );

        when(checkoutSessionRepository.findByPublicId("cs_test123")).thenReturn(Optional.of(session));
        when(paymentRepository.findByCheckoutSessionId(session.getId())).thenReturn(Optional.of(payment));
        when(evidenceService.registerCustomerRaw(
            eq(payment),
            eq(EvidenceKind.SMS),
            eq(EvidenceIngestSource.HOSTED_CHECKOUT),
            eq("MPESA"),
            eq("Confirmado ABC123. Transferiste 1.00MT."),
            eq(null),
            any(Instant.class)
        )).thenReturn(evidence);

        CustomerMessageEvidenceIngestionService service = new CustomerMessageEvidenceIngestionService(
            checkoutSessionRepository,
            paymentRepository,
            evidenceService,
            eventPublisher
        );

        CustomerMessageEvidenceView result = service.ingest(
            "cs_test123",
            "Confirmado ABC123. Transferiste 1.00MT."
        );

        assertThat(result.id()).isEqualTo("ev_customer123");
        assertThat(result.checkoutSessionId()).isEqualTo("cs_test123");
        assertThat(result.paymentId()).isEqualTo("pay_test123");
        assertThat(result.origin()).isEqualTo("CUSTOMER");
        assertThat(result.kind()).isEqualTo("SMS");
        assertThat(result.ingestSource()).isEqualTo("HOSTED_CHECKOUT");
        assertThat(result.provider()).isEqualTo("MPESA");
        verify(evidenceService).registerCustomerRaw(
            eq(payment),
            eq(EvidenceKind.SMS),
            eq(EvidenceIngestSource.HOSTED_CHECKOUT),
            eq("MPESA"),
            eq("Confirmado ABC123. Transferiste 1.00MT."),
            eq(null),
            any(Instant.class)
        );

        ArgumentCaptor<EvidenceIngestedEvent> eventCaptor = ArgumentCaptor.forClass(EvidenceIngestedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        EvidenceIngestedEvent event = eventCaptor.getValue();
        assertThat(event.merchantId()).isEqualTo(merchant.getId());
        assertThat(event.paymentId()).isEqualTo(payment.getId());
        assertThat(event.origin()).isEqualTo(EvidenceOrigin.CUSTOMER);
        assertThat(event.evidencePublicId()).isEqualTo("ev_customer123");
    }

    @Test
    void rejectsExpiredCheckoutBeforePersistingCustomerEvidence() {
        CheckoutSessionRepository checkoutSessionRepository = mock(CheckoutSessionRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        EvidenceService evidenceService = mock(EvidenceService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        Merchant merchant = new Merchant("Event Merchant");
        CheckoutSession session = session(merchant, Instant.now().minusSeconds(60));
        when(checkoutSessionRepository.findByPublicId("cs_test123")).thenReturn(Optional.of(session));

        CustomerMessageEvidenceIngestionService service = new CustomerMessageEvidenceIngestionService(
            checkoutSessionRepository,
            paymentRepository,
            evidenceService,
            eventPublisher
        );

        assertThatThrownBy(() -> service.ingest("cs_test123", "M-Pesa message"))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("expired");

        verify(paymentRepository, never()).findByCheckoutSessionId(any());
        verify(evidenceService, never()).registerCustomerRaw(any(), any(), any(), any(), any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    private CheckoutSession session(Merchant merchant, Instant expiresAt) {
        return new CheckoutSession(
            "cs_test123",
            merchant,
            ApiKeyEnvironment.TEST,
            "ORDER-TEST",
            "Test checkout",
            100,
            "MZN",
            "https://merchant.example/success",
            "https://merchant.example/cancel",
            "idem-test",
            "a".repeat(64),
            expiresAt
        );
    }
}
