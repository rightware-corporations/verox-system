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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentFeedServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock PilotManualPaymentAcceptanceRepository manualAcceptanceRepository;

    private PaymentFeedService service;
    private MerchantPrincipal principal;

    @BeforeEach
    void setUp() {
        service = new PaymentFeedService(
            paymentRepository,
            manualAcceptanceRepository,
            new MoneyConverter()
        );
        principal = new MerchantPrincipal(
            UUID.randomUUID(),
            "Owen de Jesus",
            UUID.randomUUID(),
            ApiKeyEnvironment.TEST
        );
    }

    @Test
    void listsAttentionRequiredPaymentsWithinMerchantAndEnvironmentScope() {
        Payment payment = payment("pay_pending", PaymentStatus.PENDING);
        when(paymentRepository.findAttentionRequired(
            eq(principal.merchantId()),
            eq(ApiKeyEnvironment.TEST),
            any(),
            any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(payment)));
        when(manualAcceptanceRepository.findAllByMerchantIdAndPaymentIdIn(
            eq(principal.merchantId()),
            any()
        )).thenReturn(List.of());

        PaymentPageView result = service.list(principal, null, true, 0, 20);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().id()).isEqualTo("pay_pending");
        assertThat(result.items().getFirst().attentionRequired()).isTrue();
        assertThat(result.items().getFirst().effectiveStatus()).isEqualTo("PENDING");
        verify(paymentRepository).findAttentionRequired(
            eq(principal.merchantId()),
            eq(ApiKeyEnvironment.TEST),
            any(),
            any(Pageable.class)
        );
    }

    @Test
    void manualAcceptanceChangesEffectiveStatusAndRemovesAttentionFlag() {
        Payment payment = payment("pay_manual", PaymentStatus.PENDING);
        PilotManualPaymentAcceptance acceptance = mock(PilotManualPaymentAcceptance.class);
        when(acceptance.getPaymentId()).thenReturn(payment.getId());
        when(acceptance.getAcceptedAt()).thenReturn(Instant.parse("2026-08-23T15:00:00Z"));
        when(paymentRepository.findAllByMerchantIdAndEnvironment(
            eq(principal.merchantId()),
            eq(ApiKeyEnvironment.TEST),
            any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(payment)));
        when(manualAcceptanceRepository.findAllByMerchantIdAndPaymentIdIn(
            eq(principal.merchantId()),
            any()
        )).thenReturn(List.of(acceptance));

        PaymentPageView result = service.list(principal, null, false, 0, 20);

        var item = result.items().getFirst();
        assertThat(item.status()).isEqualTo("PENDING");
        assertThat(item.effectiveStatus()).isEqualTo("MANUALLY_ACCEPTED");
        assertThat(item.attentionRequired()).isFalse();
        assertThat(item.manuallyAcceptedAt()).isEqualTo(Instant.parse("2026-08-23T15:00:00Z"));
    }

    @Test
    void rejectsInvalidStatusFilter() {
        assertThatThrownBy(() -> service.list(principal, "NOT_A_STATUS", false, 0, 20))
            .isInstanceOf(ApiException.class)
            .satisfies(error -> {
                ApiException api = (ApiException) error;
                assertThat(api.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                assertThat(api.getCode()).isEqualTo("INVALID_PAYMENT_STATUS");
            });
    }

    private Payment payment(String publicId, PaymentStatus status) {
        Payment payment = mock(Payment.class);
        CheckoutSession checkout = mock(CheckoutSession.class);
        UUID id = UUID.randomUUID();
        when(payment.getId()).thenReturn(id);
        when(payment.getPublicId()).thenReturn(publicId);
        when(payment.getStatus()).thenReturn(status);
        when(payment.getCheckoutSession()).thenReturn(checkout);
        when(checkout.getPublicId()).thenReturn("cs_" + publicId);
        when(checkout.getExternalReference()).thenReturn("TB-ORDER-001");
        when(checkout.getDescription()).thenReturn("Bilhete The Board");
        when(payment.getAmountMinor()).thenReturn(150000L);
        when(payment.getCurrency()).thenReturn("MZN");
        when(payment.getCreatedAt()).thenReturn(Instant.parse("2026-08-23T14:00:00Z"));
        return payment;
    }
}
