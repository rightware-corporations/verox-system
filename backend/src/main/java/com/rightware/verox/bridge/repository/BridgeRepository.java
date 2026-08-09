package com.rightware.verox.bridge.repository;

import com.rightware.verox.bridge.domain.Bridge;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BridgeRepository extends JpaRepository<Bridge, UUID> {

    @EntityGraph(attributePaths = {"merchant"})
    Optional<Bridge> findByPublicId(String publicId);

    @EntityGraph(attributePaths = {"merchant"})
    Optional<Bridge> findByIdAndStatus(UUID id, com.rightware.verox.bridge.domain.BridgeStatus status);

    Optional<Bridge> findByMerchantIdAndNameIgnoreCase(UUID merchantId, String name);
}
