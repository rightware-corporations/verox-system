package com.rightware.verox.paymentchannel.application;

import com.rightware.verox.authentication.domain.ApiKeyEnvironment;
import com.rightware.verox.paymentchannel.domain.MerchantPaymentChannel;
import com.rightware.verox.paymentchannel.domain.PaymentChannelStatus;
import com.rightware.verox.paymentchannel.repository.MerchantPaymentChannelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentChannelServiceTest {

    @Mock MerchantPaymentChannelRepository repository;

    private PaymentChannelService service;

    @BeforeEach
    void setUp() {
        service = new PaymentChannelService(repository);
    }

    @Test
    void listsAllChannelsInsideMerchantAndEnvironmentScope() {
        UUID merchantId = UUID.randomUUID();
        MerchantPaymentChannel channel = mockChannel("EMOLA", PaymentChannelStatus.INACTIVE);

        when(repository.findAllByMerchantIdAndEnvironmentOrderByProviderAsc(
            merchantId,
            ApiKeyEnvironment.TEST
        )).thenReturn(List.of(channel));

        List<PaymentChannelView> result = service.listForMerchant(
            merchantId,
            ApiKeyEnvironment.TEST
        );

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().provider()).isEqualTo("EMOLA");
        assertThat(result.getFirst().status()).isEqualTo("INACTIVE");
        verify(repository).findAllByMerchantIdAndEnvironmentOrderByProviderAsc(
            merchantId,
            ApiKeyEnvironment.TEST
        );
    }

    @Test
    void checkoutReadsOnlyActiveChannelsInsideMerchantAndEnvironmentScope() {
        UUID merchantId = UUID.randomUUID();
        MerchantPaymentChannel channel = mockChannel("MPESA", PaymentChannelStatus.ACTIVE);

        when(repository.findAllByMerchantIdAndEnvironmentAndStatusOrderByProviderAsc(
            merchantId,
            ApiKeyEnvironment.TEST,
            PaymentChannelStatus.ACTIVE
        )).thenReturn(List.of(channel));

        List<PaymentChannelView> result = service.listActiveForCheckout(
            merchantId,
            ApiKeyEnvironment.TEST
        );

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().provider()).isEqualTo("MPESA");
        assertThat(result.getFirst().status()).isEqualTo("ACTIVE");
        verify(repository).findAllByMerchantIdAndEnvironmentAndStatusOrderByProviderAsc(
            merchantId,
            ApiKeyEnvironment.TEST,
            PaymentChannelStatus.ACTIVE
        );
    }

    private MerchantPaymentChannel mockChannel(String provider, PaymentChannelStatus status) {
        MerchantPaymentChannel channel = mock(MerchantPaymentChannel.class);
        when(channel.getProvider()).thenReturn(provider);
        when(channel.getDisplayName()).thenReturn(provider);
        when(channel.getKind()).thenReturn("Mobile money");
        when(channel.getStatus()).thenReturn(status);
        when(channel.getRecipientDisplay()).thenReturn("receiver-display");
        when(channel.getRecipientName()).thenReturn("Recipient");
        when(channel.getInstructions()).thenReturn("Use the displayed receiver details.");
        when(channel.getUpdatedAt()).thenReturn(Instant.parse("2026-08-23T00:00:00Z"));
        return channel;
    }
}
