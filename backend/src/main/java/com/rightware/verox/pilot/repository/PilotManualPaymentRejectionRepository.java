package com.rightware.verox.pilot.repository;

import com.rightware.verox.pilot.domain.PilotManualPaymentRejection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PilotManualPaymentRejectionRepository extends JpaRepository<PilotManualPaymentRejection, UUID> {
    Optional<PilotManualPaymentRejection> findByPaymentIdAndMerchantId(UUID paymentId, UUID merchantId);
}
