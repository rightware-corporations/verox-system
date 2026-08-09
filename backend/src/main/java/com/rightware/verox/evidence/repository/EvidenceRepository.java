package com.rightware.verox.evidence.repository;

import com.rightware.verox.evidence.domain.Evidence;
import com.rightware.verox.evidence.domain.EvidenceKind;
import com.rightware.verox.evidence.domain.EvidenceOrigin;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvidenceRepository extends JpaRepository<Evidence, UUID> {

    @EntityGraph(attributePaths = {"merchant", "payment"})
    Optional<Evidence> findByPublicIdAndMerchantId(String publicId, UUID merchantId);

    @EntityGraph(attributePaths = {"merchant", "payment"})
    List<Evidence> findAllByPaymentIdOrderByReceivedAtAsc(UUID paymentId);

    boolean existsByPaymentIdAndOriginAndKindAndContentSha256(
        UUID paymentId,
        EvidenceOrigin origin,
        EvidenceKind kind,
        String contentSha256
    );
}
