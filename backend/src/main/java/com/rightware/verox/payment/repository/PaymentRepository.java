package com.rightware.verox.payment.repository;

import com.rightware.verox.payment.domain.Payment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    @EntityGraph(attributePaths = {"merchant", "checkoutSession"})
    Optional<Payment> findByCheckoutSessionId(UUID checkoutSessionId);

    @EntityGraph(attributePaths = {"merchant", "checkoutSession"})
    Optional<Payment> findByPublicIdAndMerchantId(String publicId, UUID merchantId);
}
