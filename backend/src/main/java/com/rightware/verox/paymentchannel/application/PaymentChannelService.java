package com.rightware.verox.paymentchannel.application;

import com.rightware.verox.authentication.domain.ApiKeyEnvironment;
import com.rightware.verox.paymentchannel.domain.MerchantPaymentChannel;
import com.rightware.verox.paymentchannel.domain.PaymentChannelStatus;
import com.rightware.verox.paymentchannel.repository.MerchantPaymentChannelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PaymentChannelService {

    private final MerchantPaymentChannelRepository repository;

    public PaymentChannelService(MerchantPaymentChannelRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<PaymentChannelView> listForMerchant(UUID merchantId, ApiKeyEnvironment environment) {
        return repository
            .findAllByMerchantIdAndEnvironmentOrderByProviderAsc(merchantId, environment)
            .stream()
            .map(this::toView)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentChannelView> listActiveForCheckout(UUID merchantId, ApiKeyEnvironment environment) {
        return repository
            .findAllByMerchantIdAndEnvironmentAndStatusOrderByProviderAsc(
                merchantId,
                environment,
                PaymentChannelStatus.ACTIVE
            )
            .stream()
            .map(this::toView)
            .toList();
    }

    private PaymentChannelView toView(MerchantPaymentChannel channel) {
        return new PaymentChannelView(
            channel.getProvider(),
            channel.getDisplayName(),
            channel.getKind(),
            channel.getStatus().name(),
            channel.getRecipientDisplay(),
            channel.getRecipientName(),
            channel.getInstructions(),
            channel.getUpdatedAt()
        );
    }
}
