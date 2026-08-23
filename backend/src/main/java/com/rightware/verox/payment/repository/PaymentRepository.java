package com.rightware.verox.payment.repository;

import com.rightware.verox.authentication.domain.ApiKeyEnvironment;
import com.rightware.verox.payment.domain.Payment;
import com.rightware.verox.payment.domain.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    List<Payment> findAllByMerchantIdAndEnvironmentAndStatusInOrderByCreatedAtAsc(
        UUID merchantId,
        ApiKeyEnvironment environment,
        Collection<PaymentStatus> statuses
    );

    @EntityGraph(attributePaths = {"merchant", "checkoutSession"})
    Page<Payment> findAllByMerchantIdAndEnvironment(
        UUID merchantId,
        ApiKeyEnvironment environment,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"merchant", "checkoutSession"})
    Page<Payment> findAllByMerchantIdAndEnvironmentAndStatus(
        UUID merchantId,
        ApiKeyEnvironment environment,
        PaymentStatus status,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"merchant", "checkoutSession"})
    Page<Payment> findAllByMerchantIdAndEnvironmentAndStatusIn(
        UUID merchantId,
        ApiKeyEnvironment environment,
        Collection<PaymentStatus> statuses,
        Pageable pageable
    );

    boolean existsByMerchantIdAndEnvironmentAndProviderIgnoreCaseAndProviderTransactionReferenceIgnoreCaseAndIdNot(
        UUID merchantId,
        ApiKeyEnvironment environment,
        String provider,
        String providerTransactionReference,
        UUID id
    );
}
