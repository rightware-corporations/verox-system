package com.rightware.verox.payment.application;

import com.rightware.verox.authentication.application.MerchantPrincipal;
import com.rightware.verox.authentication.domain.ApiKeyEnvironment;
import com.rightware.verox.checkout.domain.CheckoutSession;
import com.rightware.verox.common.money.MoneyConverter;
import com.rightware.verox.common.web.ApiException;
import com.rightware.verox.payment.domain.Payment;
import com.rightware.verox.payment.domain.PaymentStatus;
import com.rightware.verox.payment.repository.PaymentRepository;
import com.rightware.verox.pilot.domain.PilotManualPaymentAcceptance;
import com.rightware.verox.pilot.repository.PilotManualPaymentAcceptanceRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock PilotManualPaymentAcceptanceRepository manualAcceptanceRepository;

    private PaymentService service;
    private UUID merchantId;
    private UUID apiKeyId;
    private MerchantPrincipal principal;

    @BeforeEach
    void setUp() {
        service = new PaymentService(
            paymentRepository,
            new MoneyConverter(),
            manualAcceptanceRepository
        );
        merchantId = UUID.randomUUID();
        apiKeyId = UUID.randomUUID();
        principal = new MerchantPrincipal(
            merchantId, "Pilot Merchant", apiKeyId, ApiKeyEnvironment.TEST
        );
    }

    @Test
    void pendingPaymentWithoutManualAcceptanceKeepsPendingEffectiveStatus() {
        Payment payment = paymentForView("pay_pending", PaymentStatus.PENDING);
        when(manualAcceptanceRepository.findByPaymentIdAndMerchantId(payment.getId(), merchantId))
            .thenReturn(Optional.empty());

        PaymentView view = service.getForMerchant(principal, "pay_pending");

        assertThat(view.status()).isEqualTo("PENDING");
        assertThat(view.effectiveStatus()).isEqualTo("PENDING");
        assertThat(view.manuallyAcceptedAt()).isNull();
    }

    @Test
    void pendingPaymentWithManualAcceptanceExposesOperationalStatus() {
        Payment payment = paymentForView("pay_manual", PaymentStatus.PENDING);
        PilotManualPaymentAcceptance acceptance = new PilotManualPaymentAcceptance(
            payment.getId(), merchantId, apiKeyId, "Seen on receiving phone"
        );
        when(manualAcceptanceRepository.findByPaymentIdAndMerchantId(payment.getId(), merchantId))
            .thenReturn(Optional.of(acceptance));

        PaymentView view = service.getForMerchant(principal, "pay_manual");

        assertThat(view.status()).isEqualTo("PENDING");
        assertThat(view.effectiveStatus()).isEqualTo("MANUALLY_ACCEPTED");
        assertThat(view.manuallyAcceptedAt()).isEqualTo(acceptance.getAcceptedAt());
    }

    @Test
    void confirmedPaymentAlwaysExposesConfirmedAsEffectiveStatus() {
        Payment payment = paymentForView("pay_confirmed", PaymentStatus.CONFIRMED);
        PilotManualPaymentAcceptance acceptance = new PilotManualPaymentAcceptance(
            payment.getId(), merchantId, apiKeyId, "Previously accepted manually"
        );
        when(manualAcceptanceRepository.findByPaymentIdAndMerchantId(payment.getId(), merchantId))
            .thenReturn(Optional.of(acceptance));

        PaymentView view = service.getForMerchant(principal, "pay_confirmed");

        assertThat(view.status()).isEqualTo("CONFIRMED");
        assertThat(view.effectiveStatus()).isEqualTo("CONFIRMED");
        assertThat(view.manuallyAcceptedAt()).isEqualTo(acceptance.getAcceptedAt());
    }

    @Test
    void testCredentialCannotReadLivePayment() {
        Payment payment = org.mockito.Mockito.mock(Payment.class);
        when(paymentRepository.findByPublicIdAndMerchantId("pay_live", merchantId))
            .thenReturn(Optional.of(payment));
        when(payment.getEnvironment()).thenReturn(ApiKeyEnvironment.LIVE);

        assertThatThrownBy(() -> service.getForMerchant(principal, "pay_live"))
            .isInstanceOf(ApiException.class)
            .satisfies(error -> {
                ApiException api = (ApiException) error;
                assertThat(api.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(api.getCode()).isEqualTo("PAYMENT_NOT_FOUND");
            });

        verify(manualAcceptanceRepository, never())
            .findByPaymentIdAndMerchantId(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private Payment paymentForView(String publicId, PaymentStatus status) {
        Payment payment = org.mockito.Mockito.mock(Payment.class);
        CheckoutSession checkout = org.mockito.Mockito.mock(CheckoutSession.class);
        UUID paymentId = UUID.randomUUID();

        when(paymentRepository.findByPublicIdAndMerchantId(publicId, merchantId))
            .thenReturn(Optional.of(payment));
        when(payment.getId()).thenReturn(paymentId);
        when(payment.getEnvironment()).thenReturn(ApiKeyEnvironment.TEST);
        when(payment.getStatus()).thenReturn(status);
        when(payment.getPublicId()).thenReturn(publicId);
        when(payment.getCheckoutSession()).thenReturn(checkout);
        when(checkout.getPublicId()).thenReturn("cs_test");
        when(checkout.getExternalReference()).thenReturn("ORDER-TEST");
        when(payment.getAmountMinor()).thenReturn(100L);
        when(payment.getCurrency()).thenReturn("MZN");

        return payment;
    }
}
