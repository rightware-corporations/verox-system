package com.rightware.verox.paymentchannel.repository;

import com.rightware.verox.authentication.domain.ApiKeyEnvironment;
import com.rightware.verox.paymentchannel.domain.MerchantPaymentChannel;
import com.rightware.verox.paymentchannel.domain.PaymentChannelStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MerchantPaymentChannelRepository extends JpaRepository<MerchantPaymentChannel, UUID> {

    @EntityGraph(attributePaths = "merchant")
    List<MerchantPaymentChannel> findAllByMerchantIdAndEnvironmentOrderByProviderAsc(
        UUID merchantId,
        ApiKeyEnvironment environment
    );

    @EntityGraph(attributePaths = "merchant")
    List<MerchantPaymentChannel> findAllByMerchantIdAndEnvironmentAndStatusOrderByProviderAsc(
        UUID merchantId,
        ApiKeyEnvironment environment,
        PaymentChannelStatus status
    );
}
