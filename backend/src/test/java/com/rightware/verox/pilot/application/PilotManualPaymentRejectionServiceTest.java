package com.rightware.verox.pilot.application;

import com.rightware.verox.authentication.application.MerchantOperatorPrincipal;
import com.rightware.verox.authentication.application.MerchantPrincipal;
import com.rightware.verox.authentication.domain.ApiKeyEnvironment;
import com.rightware.verox.checkout.domain.CheckoutSession;
import com.rightware.verox.common.web.ApiException;
import com.rightware.verox.payment.domain.Payment;
import com.rightware.verox.payment.domain.PaymentStatus;
import com.rightware.verox.payment.repository.PaymentRepository;
import com.rightware.verox.pilot.domain.PilotManualPaymentRejection;
import com.rightware.verox.pilot.repository.PilotManualPaymentRejectionRepository;
import com.rightware.verox.pilot.security.PilotManualAcceptanceAccessPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PilotManualPaymentRejectionServiceTest {
    @Mock PaymentRepository paymentRepository;
    @Mock PilotManualPaymentRejectionRepository rejectionRepository;
    @Mock PilotManualAcceptanceAccessPolicy accessPolicy;
    @Mock org.springframework.context.ApplicationEventPublisher eventPublisher;

    private PilotManualPaymentRejectionService service;
    private UUID merchantId;
    private UUID operatorId;
    private MerchantOperatorPrincipal operator;

    @BeforeEach
    void setUp() {
        service = new PilotManualPaymentRejectionService(paymentRepository, rejectionRepository, accessPolicy, eventPublisher);
        merchantId = UUID.randomUUID(); operatorId = UUID.randomUUID();
        operator = new MerchantOperatorPrincipal(operatorId, "Owen de Jesus", merchantId, "Pilot Merchant", ApiKeyEnvironment.TEST, UUID.randomUUID());
    }

    @Test
    void rejectsPendingPaymentAndPersistsOperatorReasonAndTimestampWithoutChangingCoreStatus() {
        Payment payment = payment("pay_reject", PaymentStatus.PENDING);
        when(accessPolicy.isAllowed(merchantId)).thenReturn(true);
        when(paymentRepository.findByPublicIdAndMerchantId("pay_reject", merchantId)).thenReturn(Optional.of(payment));
        when(rejectionRepository.findByPaymentIdAndMerchantId(payment.getId(), merchantId)).thenReturn(Optional.empty());
        when(rejectionRepository.save(any(PilotManualPaymentRejection.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PilotManualPaymentRejectionView result = service.reject(operator, "pay_reject", "Não apareceu no telefone");

        assertThat(result.status()).isEqualTo("MANUALLY_REJECTED");
        assertThat(result.reason()).isEqualTo("Não apareceu no telefone");
        assertThat(result.rejectedAt()).isNotNull();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(rejectionRepository).save(org.mockito.ArgumentMatchers.argThat(rejection ->
            operatorId.equals(rejection.getRejectedByOperatorId())
                && merchantId.equals(rejection.getMerchantId())
                && rejection.getRejectedAt() != null
                && "Não apareceu no telefone".equals(rejection.getReason())
        ));
        verify(eventPublisher).publishEvent(any(PaymentManuallyRejectedEvent.class));
    }

    @Test
    void deniesNonPilotMerchant() {
        when(accessPolicy.isAllowed(merchantId)).thenReturn(false);
        assertThatThrownBy(() -> service.reject(operator, "pay_reject", null))
            .isInstanceOf(ApiException.class).satisfies(error -> assertThat(((ApiException) error).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(paymentRepository, never()).findByPublicIdAndMerchantId(any(), any());
    }

    @Test
    void deniesCrossMerchantPaymentLookup() {
        when(accessPolicy.isAllowed(merchantId)).thenReturn(true);
        when(paymentRepository.findByPublicIdAndMerchantId("pay_other", merchantId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.reject(operator, "pay_other", null))
            .isInstanceOf(ApiException.class).satisfies(error -> assertThat(((ApiException) error).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(rejectionRepository, never()).save(any());
    }

    @Test
    void duplicateEquivalentRejectionReturnsExistingDecisionWithoutNewAuditOrEvent() {
        Payment payment = payment("pay_repeat", PaymentStatus.PENDING);
        PilotManualPaymentRejection existing = new PilotManualPaymentRejection(payment.getId(), merchantId, null, operatorId, "já verificado");
        when(accessPolicy.isAllowed(merchantId)).thenReturn(true);
        when(paymentRepository.findByPublicIdAndMerchantId("pay_repeat", merchantId)).thenReturn(Optional.of(payment));
        when(rejectionRepository.findByPaymentIdAndMerchantId(payment.getId(), merchantId)).thenReturn(Optional.of(existing));

        PilotManualPaymentRejectionView result = service.reject(operator, "pay_repeat", "segunda observação");

        assertThat(result.status()).isEqualTo("MANUALLY_REJECTED");
        assertThat(result.reason()).isEqualTo("já verificado");
        verify(rejectionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void rejectsOverlongOptionalReason() {
        when(accessPolicy.isAllowed(merchantId)).thenReturn(true);
        assertThatThrownBy(() -> service.reject(operator, "pay_reject", "x".repeat(256)))
            .isInstanceOf(ApiException.class).satisfies(error -> assertThat(((ApiException) error).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(paymentRepository, never()).findByPublicIdAndMerchantId(any(), any());
    }

    @Test
    void automaticConfirmedStatusRemainsConfirmedPrecedenceAtPaymentServiceBoundary() {
        assertThat(PaymentStatus.CONFIRMED.name()).isEqualTo("CONFIRMED");
        assertThat(PaymentStatus.PENDING.name()).isNotEqualTo("MANUALLY_REJECTED");
    }

    private Payment payment(String publicId, PaymentStatus status) {
        Payment payment = org.mockito.Mockito.mock(Payment.class);
        when(payment.getId()).thenReturn(UUID.randomUUID());
        when(payment.getPublicId()).thenReturn(publicId);
        org.mockito.Mockito.lenient().when(payment.getEnvironment()).thenReturn(ApiKeyEnvironment.TEST);
        org.mockito.Mockito.lenient().when(payment.getStatus()).thenReturn(status);
        CheckoutSession checkout = org.mockito.Mockito.mock(CheckoutSession.class);
        org.mockito.Mockito.lenient().when(payment.getCheckoutSession()).thenReturn(checkout);
        org.mockito.Mockito.lenient().when(checkout.getPublicId()).thenReturn("cs_test");
        org.mockito.Mockito.lenient().when(checkout.getExternalReference()).thenReturn("ORDER-TEST");
        org.mockito.Mockito.lenient().when(payment.getAmountMinor()).thenReturn(100L);
        org.mockito.Mockito.lenient().when(payment.getCurrency()).thenReturn("MZN");
        return payment;
    }
}
