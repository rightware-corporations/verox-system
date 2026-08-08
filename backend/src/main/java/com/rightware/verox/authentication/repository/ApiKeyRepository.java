package com.rightware.verox.authentication.repository;

import com.rightware.verox.authentication.domain.ApiKey;
import com.rightware.verox.authentication.domain.ApiKeyStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    @EntityGraph(attributePaths = "merchant")
    Optional<ApiKey> findByKeyHashAndStatus(String keyHash, ApiKeyStatus status);
}
