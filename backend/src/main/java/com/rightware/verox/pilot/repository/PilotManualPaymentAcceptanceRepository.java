package com.rightware.verox.pilot.repository;

import com.rightware.verox.pilot.domain.PilotManualPaymentAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PilotManualPaymentAcceptanceRepository
    extends JpaRepository<PilotManualPaymentAcceptance, UUID> {

    Optional<PilotManualPaymentAcceptance> findByPaymentIdAndMerchantId(
        UUID paymentId,
        UUID merchantId
    );

    boolean existsByPaymentId(UUID paymentId);
}
