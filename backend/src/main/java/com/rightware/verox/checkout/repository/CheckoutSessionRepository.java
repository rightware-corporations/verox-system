package com.rightware.verox.checkout.repository;

import com.rightware.verox.checkout.domain.CheckoutSession;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CheckoutSessionRepository extends JpaRepository<CheckoutSession, UUID> {

    @EntityGraph(attributePaths = "merchant")
    Optional<CheckoutSession> findByMerchantIdAndIdempotencyKey(UUID merchantId, String idempotencyKey);

    @EntityGraph(attributePaths = "merchant")
    Optional<CheckoutSession> findByPublicIdAndMerchantId(String publicId, UUID merchantId);

    Optional<CheckoutSession> findByPublicId(String publicId);
}
