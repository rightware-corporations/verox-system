package com.rightware.verox.evidence.repository;

import com.rightware.verox.authentication.domain.ApiKeyEnvironment;
import com.rightware.verox.evidence.domain.Evidence;
import com.rightware.verox.evidence.domain.EvidenceKind;
import com.rightware.verox.evidence.domain.EvidenceOrigin;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvidenceRepository extends JpaRepository<Evidence, UUID> {

    @EntityGraph(attributePaths = {"merchant", "payment"})
    Optional<Evidence> findByPublicIdAndMerchantId(String publicId, UUID merchantId);

    @EntityGraph(attributePaths = {"merchant", "payment"})
    List<Evidence> findAllByPaymentIdOrderByReceivedAtAsc(UUID paymentId);

    @EntityGraph(attributePaths = {"merchant", "payment"})
    List<Evidence> findAllByMerchantIdAndEnvironmentAndOriginAndProviderIgnoreCaseAndPaymentIsNullAndCreatedAtBetweenOrderByCreatedAtAsc(
        UUID merchantId,
        ApiKeyEnvironment environment,
        EvidenceOrigin origin,
        String provider,
        Instant createdAtFrom,
        Instant createdAtTo
    );

    @EntityGraph(attributePaths = {"merchant", "payment"})
    Optional<Evidence> findByPaymentIdAndOriginAndKindAndContentSha256(
        UUID paymentId,
        EvidenceOrigin origin,
        EvidenceKind kind,
        String contentSha256
    );

    @EntityGraph(attributePaths = {"merchant", "payment"})
    Optional<Evidence> findByMerchantIdAndEnvironmentAndOriginAndKindAndContentSha256(
        UUID merchantId,
        ApiKeyEnvironment environment,
        EvidenceOrigin origin,
        EvidenceKind kind,
        String contentSha256
    );
}
