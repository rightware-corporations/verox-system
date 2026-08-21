package com.rightware.verox.pilot.application;

import com.rightware.verox.authentication.application.MerchantPrincipal;
import com.rightware.verox.authentication.domain.ApiKeyEnvironment;
import com.rightware.verox.common.web.ApiException;
import com.rightware.verox.payment.domain.Payment;
import com.rightware.verox.payment.domain.PaymentStatus;
import com.rightware.verox.payment.repository.PaymentRepository;
import com.rightware.verox.pilot.domain.PilotManualPaymentAcceptance;
import com.rightware.verox.pilot.repository.PilotManualPaymentAcceptanceRepository;
import com.rightware.verox.pilot.security.PilotManualAcceptanceAccessPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PilotManualPaymentAcceptanceServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock PilotManualPaymentAcceptanceRepository acceptanceRepository;
    @Mock PilotManualAcceptanceAccessPolicy accessPolicy;
    @Mock org.springframework.context.ApplicationEventPublisher eventPublisher;

    private PilotManualPaymentAcceptanceService service;

    private UUID merchantId;
    private UUID apiKeyId;
    private MerchantPrincipal principal;

    @BeforeEach
    void setUp() {
        service = new PilotManualPaymentAcceptanceService(
            paymentRepository, acceptanceRepository, accessPolicy, eventPublisher
        );
        merchantId = UUID.randomUUID();
        apiKeyId = UUID.randomUUID();
        principal = new MerchantPrincipal(
            merchantId, "Pilot Merchant", apiKeyId, ApiKeyEnvironment.TEST
        );
    }

    @Test
    void deniesMerchantThatIsNotAllowlisted() {
        when(accessPolicy.isAllowed(merchantId)).thenReturn(false);

        assertThatThrownBy(() -> service.accept(principal, "pay_test", null))
            .isInstanceOf(ApiException.class)
            .satisfies(error -> {
                ApiException api = (ApiException) error;
                assertThat(api.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                assertThat(api.getCode()).isEqualTo("PILOT_MANUAL_ACCEPTANCE_FORBIDDEN");
            });

        verify(paymentRepository, never()).findByPublicIdAndMerchantId(any(), any());
    }

    @Test
    void cannotAccessPaymentOutsideAuthenticatedMerchantScope() {
        when(accessPolicy.isAllowed(merchantId)).thenReturn(true);
        when(paymentRepository.findByPublicIdAndMerchantId("pay_other", merchantId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.accept(principal, "pay_other", null))
            .isInstanceOf(ApiException.class)
            .satisfies(error -> {
                ApiException api = (ApiException) error;
                assertThat(api.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(api.getCode()).isEqualTo("PAYMENT_NOT_FOUND");
            });

        verify(paymentRepository).findByPublicIdAndMerchantId("pay_other", merchantId);
        verify(acceptanceRepository, never()).save(any());
    }

    @Test
    void cannotAcceptPaymentFromDifferentEnvironment() {
        Payment payment = org.mockito.Mockito.mock(Payment.class);
        when(accessPolicy.isAllowed(merchantId)).thenReturn(true);
        when(paymentRepository.findByPublicIdAndMerchantId("pay_live", merchantId))
            .thenReturn(Optional.of(payment));
        when(payment.getEnvironment()).thenReturn(ApiKeyEnvironment.LIVE);

        assertThatThrownBy(() -> service.accept(principal, "pay_live", null))
            .isInstanceOf(ApiException.class)
            .satisfies(error -> {
                ApiException api = (ApiException) error;
                assertThat(api.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(api.getCode()).isEqualTo("PAYMENT_NOT_FOUND");
            });

        verify(acceptanceRepository, never()).save(any());
    }

    @Test
    void cannotManuallyAcceptConfirmedPayment() {
        Payment payment = eligiblePayment("pay_confirmed");
        when(acceptanceRepository.findByPaymentIdAndMerchantId(payment.getId(), merchantId))
            .thenReturn(Optional.empty());
        when(payment.getStatus()).thenReturn(PaymentStatus.CONFIRMED);

        assertThatThrownBy(() -> service.accept(principal, "pay_confirmed", null))
            .isInstanceOf(ApiException.class)
            .satisfies(error -> {
                ApiException api = (ApiException) error;
                assertThat(api.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                assertThat(api.getCode()).isEqualTo("PILOT_MANUAL_ACCEPTANCE_NOT_ALLOWED");
            });

        verify(acceptanceRepository, never()).save(any());
    }

    @Test
    void repeatedAcceptanceReturnsExistingRecordWithoutCreatingAnother() {
        Payment payment = eligiblePayment("pay_repeat");
        when(payment.getPublicId()).thenReturn("pay_repeat");
        PilotManualPaymentAcceptance existing = new PilotManualPaymentAcceptance(
            payment.getId(), merchantId, apiKeyId, "Seen on receiving phone"
        );
        when(acceptanceRepository.findByPaymentIdAndMerchantId(payment.getId(), merchantId))
            .thenReturn(Optional.of(existing));

        PilotManualPaymentAcceptanceView result =
            service.accept(principal, "pay_repeat", "ignored second reason");

        assertThat(result.status()).isEqualTo("MANUALLY_ACCEPTED");
        assertThat(result.paymentId()).isEqualTo("pay_repeat");
        verify(acceptanceRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void acceptsPendingPaymentForAuthorizedMerchant() {
        Payment payment = eligiblePayment("pay_pending");
        when(payment.getPublicId()).thenReturn("pay_pending");
        when(payment.getStatus()).thenReturn(PaymentStatus.PENDING);
        com.rightware.verox.checkout.domain.CheckoutSession checkout = org.mockito.Mockito.mock(com.rightware.verox.checkout.domain.CheckoutSession.class);
        when(payment.getCheckoutSession()).thenReturn(checkout);
        when(checkout.getPublicId()).thenReturn("cs_pending");
        when(checkout.getExternalReference()).thenReturn("ORDER-PENDING");
        when(payment.getAmountMinor()).thenReturn(100L);
        when(payment.getCurrency()).thenReturn("MZN");
        when(acceptanceRepository.findByPaymentIdAndMerchantId(payment.getId(), merchantId))
            .thenReturn(Optional.empty());
        when(acceptanceRepository.save(any(PilotManualPaymentAcceptance.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        PilotManualPaymentAcceptanceView result =
            service.accept(principal, "pay_pending", "Seen on receiving phone");

        assertThat(result.paymentId()).isEqualTo("pay_pending");
        assertThat(result.status()).isEqualTo("MANUALLY_ACCEPTED");
        assertThat(result.reason()).isEqualTo("Seen on receiving phone");
        assertThat(result.acceptedAt()).isNotNull();
        verify(eventPublisher).publishEvent(any(PaymentManuallyAcceptedEvent.class));

        verify(acceptanceRepository).save(org.mockito.ArgumentMatchers.argThat(acceptance ->
            acceptance.getPaymentId().equals(payment.getId())
                && acceptance.getMerchantId().equals(merchantId)
                && acceptance.getAcceptedByApiKeyId().equals(apiKeyId)
        ));
    }

    @Test
    void getReturnsExistingManualAcceptance() {
        Payment payment = eligiblePayment("pay_manual");
        when(payment.getPublicId()).thenReturn("pay_manual");
        PilotManualPaymentAcceptance existing = new PilotManualPaymentAcceptance(
            payment.getId(), merchantId, apiKeyId, "Confirmed on pilot phone"
        );
        when(acceptanceRepository.findByPaymentIdAndMerchantId(payment.getId(), merchantId))
            .thenReturn(Optional.of(existing));

        PilotManualPaymentAcceptanceView result =
            service.get(principal, "pay_manual");

        assertThat(result.paymentId()).isEqualTo("pay_manual");
        assertThat(result.status()).isEqualTo("MANUALLY_ACCEPTED");
        assertThat(result.reason()).isEqualTo("Confirmed on pilot phone");
    }
    private Payment eligiblePayment(String publicId) {
        Payment payment = org.mockito.Mockito.mock(Payment.class);
        UUID paymentId = UUID.randomUUID();

        when(accessPolicy.isAllowed(merchantId)).thenReturn(true);
        when(paymentRepository.findByPublicIdAndMerchantId(publicId, merchantId))
            .thenReturn(Optional.of(payment));
        when(payment.getId()).thenReturn(paymentId);
        when(payment.getEnvironment()).thenReturn(ApiKeyEnvironment.TEST);

        return payment;
    }
}
