package com.rightware.verox.pilot.repository;

import com.rightware.verox.pilot.domain.PilotManualPaymentAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PilotManualPaymentAcceptanceRepository
    extends JpaRepository<PilotManualPaymentAcceptance, UUID> {

    Optional<PilotManualPaymentAcceptance> findByPaymentIdAndMerchantId(
        UUID paymentId,
        UUID merchantId
    );

    List<PilotManualPaymentAcceptance> findAllByMerchantIdAndPaymentIdIn(
        UUID merchantId,
        Collection<UUID> paymentIds
    );

    boolean existsByPaymentId(UUID paymentId);
}
