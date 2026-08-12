package com.rightware.verox.checkout.application;

import com.rightware.verox.authentication.domain.ApiKeyEnvironment;
import com.rightware.verox.checkout.domain.CheckoutSession;
import com.rightware.verox.checkout.repository.CheckoutSessionRepository;
import com.rightware.verox.common.id.ResourceIdGenerator;
import com.rightware.verox.common.money.MoneyConverter;
import com.rightware.verox.common.web.ApiException;
import com.rightware.verox.merchant.domain.Merchant;
import com.rightware.verox.merchant.repository.MerchantRepository;
import com.rightware.verox.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CheckoutSessionServiceTest {

    @Test
    void createsCheckoutAndPaymentForAuthenticatedMerchant() {
        CheckoutSessionRepository checkoutRepository = mock(CheckoutSessionRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        MerchantRepository merchantRepository = mock(MerchantRepository.class);
        ResourceIdGenerator ids = mock(ResourceIdGenerator.class);
        CheckoutRequestFingerprint fingerprint = mock(CheckoutRequestFingerprint.class);
        CheckoutSubmissionCapabilityService capabilities = new CheckoutSubmissionCapabilityService(
            "test-checkout-capability-secret"
        );

        Merchant merchant = new Merchant("Event Merchant");
        when(merchantRepository.findById(merchant.getId())).thenReturn(Optional.of(merchant));
        when(checkoutRepository.findByMerchantIdAndIdempotencyKey(merchant.getId(), "order-100"))
            .thenReturn(Optional.empty());
        when(ids.generate("cs")).thenReturn("cs_test123");
        when(ids.generate("pay")).thenReturn("pay_test123");
        when(fingerprint.create(anyLong(), any(), any(), any(), any(), any())).thenReturn("fingerprint");

        CheckoutSessionService service = new CheckoutSessionService(
            checkoutRepository,
            paymentRepository,
            merchantRepository,
            new MoneyConverter(),
            ids,
            new RedirectUrlValidator(),
            fingerprint,
            capabilities,
            "https://checkout.verox.test/",
            15
        );

        CheckoutSessionView result = service.create(new CreateCheckoutSessionCommand(
            merchant.getId(),
            ApiKeyEnvironment.LIVE,
            "order-100",
            new BigDecimal("1500.00"),
            "MZN",
            "ORDER-100",
            "VIP Ticket",
            "https://event.test/payment/success",
            "https://event.test/payment/cancel"
        ));

        assertThat(result.id()).isEqualTo("cs_test123");
        assertThat(result.paymentId()).isEqualTo("pay_test123");
        assertThat(result.amount()).isEqualTo("1500.00");
        assertThat(result.checkoutUrl())
            .startsWith("https://checkout.verox.test/c/cs_test123#vx_capability=vx_checkout_");
        assertThat(result.status()).isEqualTo("OPEN");
        assertThat(result.paymentStatus()).isEqualTo("PENDING");
        verify(checkoutRepository).save(any());
        verify(paymentRepository).save(any());
    }

    @Test
    void rejectsReuseOfIdempotencyKeyWithDifferentRequest() {
        CheckoutSessionRepository checkoutRepository = mock(CheckoutSessionRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        MerchantRepository merchantRepository = mock(MerchantRepository.class);
        ResourceIdGenerator ids = mock(ResourceIdGenerator.class);
        CheckoutRequestFingerprint fingerprint = mock(CheckoutRequestFingerprint.class);
        CheckoutSubmissionCapabilityService capabilities = new CheckoutSubmissionCapabilityService(
            "test-checkout-capability-secret"
        );

        Merchant merchant = new Merchant("Event Merchant");
        CheckoutSession existing = new CheckoutSession(
            "cs_existing",
            merchant,
            ApiKeyEnvironment.LIVE,
            "ORDER-100",
            "VIP Ticket",
            150000,
            "MZN",
            "https://event.test/payment/success",
            "https://event.test/payment/cancel",
            "order-100",
            "old-fingerprint",
            Instant.now().plusSeconds(900)
        );

        when(merchantRepository.findById(merchant.getId())).thenReturn(Optional.of(merchant));
        when(checkoutRepository.findByMerchantIdAndIdempotencyKey(merchant.getId(), "order-100"))
            .thenReturn(Optional.of(existing));
        when(fingerprint.create(anyLong(), any(), any(), any(), any(), any())).thenReturn("new-fingerprint");

        CheckoutSessionService service = new CheckoutSessionService(
            checkoutRepository,
            paymentRepository,
            merchantRepository,
            new MoneyConverter(),
            ids,
            new RedirectUrlValidator(),
            fingerprint,
            capabilities,
            "https://checkout.verox.test",
            15
        );

        assertThatThrownBy(() -> service.create(new CreateCheckoutSessionCommand(
            merchant.getId(),
            ApiKeyEnvironment.LIVE,
            "order-100",
            new BigDecimal("1500.00"),
            "MZN",
            "ORDER-100",
            "Different description",
            "https://event.test/payment/success",
            "https://event.test/payment/cancel"
        )))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("Idempotency-Key");
    }
}
