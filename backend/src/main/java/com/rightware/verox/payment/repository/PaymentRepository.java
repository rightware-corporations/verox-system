package com.rightware.verox.payment.repository;

import com.rightware.verox.payment.domain.Payment;
import com.rightware.verox.payment.domain.PaymentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    @Override
    @EntityGraph(attributePaths = {"merchant", "checkoutSession"})
    Optional<Payment> findById(UUID id);

    @EntityGraph(attributePaths = {"merchant", "checkoutSession"})
    Optional<Payment> findByCheckoutSessionId(UUID checkoutSessionId);

    @EntityGraph(attributePaths = {"merchant", "checkoutSession"})
    Optional<Payment> findByPublicIdAndMerchantId(String publicId, UUID merchantId);

    @EntityGraph(attributePaths = {"merchant", "checkoutSession"})
    List<Payment> findAllByMerchantIdAndStatusInOrderByCreatedAtAsc(
        UUID merchantId,
        Collection<PaymentStatus> statuses
    );

    boolean existsByMerchantIdAndProviderIgnoreCaseAndProviderTransactionReferenceIgnoreCaseAndIdNot(
        UUID merchantId,
        String provider,
        String providerTransactionReference,
        UUID id
    );
}
