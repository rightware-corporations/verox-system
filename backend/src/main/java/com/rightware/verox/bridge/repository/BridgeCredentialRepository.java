package com.rightware.verox.bridge.repository;

import com.rightware.verox.bridge.domain.BridgeCredential;
import com.rightware.verox.bridge.domain.BridgeCredentialStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BridgeCredentialRepository extends JpaRepository<BridgeCredential, UUID> {

    @EntityGraph(attributePaths = {"bridge", "bridge.merchant"})
    Optional<BridgeCredential> findByKeyHashAndStatus(String keyHash, BridgeCredentialStatus status);
}
