package com.rightware.verox.checkout.application;

import com.rightware.verox.authentication.domain.ApiKeyEnvironment;
import com.rightware.verox.checkout.domain.CheckoutSession;
import com.rightware.verox.checkout.domain.CheckoutSessionStatus;
import com.rightware.verox.checkout.repository.CheckoutSessionRepository;
import com.rightware.verox.common.money.MoneyConverter;
import com.rightware.verox.common.web.ApiException;
import com.rightware.verox.merchant.domain.Merchant;
import com.rightware.verox.payment.domain.Payment;
import com.rightware.verox.payment.domain.PaymentStatus;
import com.rightware.verox.payment.repository.PaymentRepository;
import com.rightware.verox.paymentchannel.application.PaymentChannelService;
import com.rightware.verox.paymentchannel.application.PaymentChannelView;
import com.rightware.verox.pilot.domain.PilotManualPaymentAcceptance;
import com.rightware.verox.pilot.repository.PilotManualPaymentAcceptanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HostedCheckoutBootstrapServiceTest {

    @Mock CheckoutSessionRepository checkoutSessionRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock PilotManualPaymentAcceptanceRepository manualAcceptanceRepository;
    @Mock CheckoutSubmissionCapabilityService capabilityService;
    @Mock PaymentChannelService paymentChannelService;

    private HostedCheckoutBootstrapService service;

    @BeforeEach
    void setUp() {
        service = new HostedCheckoutBootstrapService(
            checkoutSessionRepository,
            paymentRepository,
            manualAcceptanceRepository,
            capabilityService,
            new MoneyConverter(),
            paymentChannelService
        );
    }

    @Test
    void invalidCapabilityIsIndistinguishableFromMissingCheckout() {
        CheckoutSession session = mock(CheckoutSession.class);
        Merchant merchant = mock(Merchant.class);
        when(checkoutSessionRepository.findByPublicId("cs_test")).thenReturn(Optional.of(session));
        when(session.getMerchant()).thenReturn(merchant);
        when(merchant.isActive()).thenReturn(true);
        when(capabilityService.matches(session, "wrong")).thenReturn(false);

        assertThatThrownBy(() -> service.get("cs_test", "wrong"))
            .isInstanceOf(ApiException.class)
            .satisfies(error -> {
                ApiException api = (ApiException) error;
                assertThat(api.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(api.getCode()).isEqualTo("CHECKOUT_SESSION_NOT_FOUND");
            });
    }

    @Test
    void returnsSafeCheckoutPresentationWithActiveChannels() {
        CheckoutSession session = mock(CheckoutSession.class);
        Merchant merchant = mock(Merchant.class);
        Payment payment = mock(Payment.class);
        UUID sessionId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(600);

        when(checkoutSessionRepository.findByPublicId("cs_test")).thenReturn(Optional.of(session));
        when(session.getMerchant()).thenReturn(merchant);
        when(merchant.isActive()).thenReturn(true);
        when(capabilityService.matches(session, "valid")).thenReturn(true);
        when(session.getId()).thenReturn(sessionId);
        when(paymentRepository.findByCheckoutSessionId(sessionId)).thenReturn(Optional.of(payment));
        when(payment.getId()).thenReturn(paymentId);
        when(merchant.getId()).thenReturn(merchantId);
        when(session.getEnvironment()).thenReturn(ApiKeyEnvironment.TEST);
        when(manualAcceptanceRepository.findByPaymentIdAndMerchantId(paymentId, merchantId))
            .thenReturn(Optional.empty());
        when(paymentChannelService.listActiveForCheckout(merchantId, ApiKeyEnvironment.TEST))
            .thenReturn(List.of(new PaymentChannelView(
                "MPESA",
                "M-Pesa",
                "Mobile money",
                "ACTIVE",
                "receiver-display",
                "Recipient",
                "Use the displayed receiver details.",
                Instant.now()
            )));

        when(session.getPublicId()).thenReturn("cs_test");
        when(payment.getPublicId()).thenReturn("pay_test");
        when(merchant.getName()).thenReturn("Owen de Jesus");
        when(session.getExternalReference()).thenReturn("TB-ORDER-001");
        when(session.getDescription()).thenReturn("2 bilhetes");
        when(session.getAmountMinor()).thenReturn(150000L);
        when(session.getCurrency()).thenReturn("MZN");
        when(session.getStatus()).thenReturn(CheckoutSessionStatus.OPEN);
        when(payment.getStatus()).thenReturn(PaymentStatus.PENDING);
        when(session.getExpiresAt()).thenReturn(expiresAt);
        when(session.getSuccessUrl()).thenReturn("https://theboard.example/success");
        when(session.getCancelUrl()).thenReturn("https://theboard.example/cancel");

        HostedCheckoutBootstrapView result = service.get("cs_test", "valid");

        assertThat(result.checkoutSessionId()).isEqualTo("cs_test");
        assertThat(result.paymentId()).isEqualTo("pay_test");
        assertThat(result.merchantDisplayName()).isEqualTo("Owen de Jesus");
        assertThat(result.externalReference()).isEqualTo("TB-ORDER-001");
        assertThat(result.amount()).isEqualTo("1500.00");
        assertThat(result.currency()).isEqualTo("MZN");
        assertThat(result.checkoutStatus()).isEqualTo("OPEN");
        assertThat(result.paymentStatus()).isEqualTo("PENDING");
        assertThat(result.effectivePaymentStatus()).isEqualTo("PENDING");
        assertThat(result.paymentChannels()).hasSize(1);
        assertThat(result.paymentChannels().getFirst().provider()).isEqualTo("MPESA");
        assertThat(result.paymentChannels().getFirst().enabled()).isTrue();
    }

    @Test
    void exposesManualAcceptanceAsEffectiveStatusWithoutChangingCoreStatus() {
        CheckoutSession session = mock(CheckoutSession.class);
        Merchant merchant = mock(Merchant.class);
        Payment payment = mock(Payment.class);
        PilotManualPaymentAcceptance acceptance = mock(PilotManualPaymentAcceptance.class);
        UUID sessionId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        when(checkoutSessionRepository.findByPublicId("cs_manual")).thenReturn(Optional.of(session));
        when(session.getMerchant()).thenReturn(merchant);
        when(merchant.isActive()).thenReturn(true);
        when(capabilityService.matches(session, "valid")).thenReturn(true);
        when(session.getId()).thenReturn(sessionId);
        when(paymentRepository.findByCheckoutSessionId(sessionId)).thenReturn(Optional.of(payment));
        when(payment.getId()).thenReturn(paymentId);
        when(merchant.getId()).thenReturn(merchantId);
        when(session.getEnvironment()).thenReturn(ApiKeyEnvironment.TEST);
        when(manualAcceptanceRepository.findByPaymentIdAndMerchantId(paymentId, merchantId))
            .thenReturn(Optional.of(acceptance));
        when(paymentChannelService.listActiveForCheckout(merchantId, ApiKeyEnvironment.TEST))
            .thenReturn(List.of());
        when(payment.getStatus()).thenReturn(PaymentStatus.PENDING);
        when(session.getStatus()).thenReturn(CheckoutSessionStatus.OPEN);
        when(session.getAmountMinor()).thenReturn(100L);
        when(session.getCurrency()).thenReturn("MZN");

        HostedCheckoutBootstrapView result = service.get("cs_manual", "valid");

        assertThat(result.paymentStatus()).isEqualTo("PENDING");
        assertThat(result.effectivePaymentStatus()).isEqualTo("MANUALLY_ACCEPTED");
    }
}
