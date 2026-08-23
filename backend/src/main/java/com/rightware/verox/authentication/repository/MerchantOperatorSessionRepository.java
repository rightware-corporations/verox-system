package com.rightware.verox.authentication.repository;

import com.rightware.verox.authentication.domain.MerchantOperatorSession;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MerchantOperatorSessionRepository extends JpaRepository<MerchantOperatorSession, UUID> {
    @EntityGraph(attributePaths = {"operator", "operator.merchant"})
    Optional<MerchantOperatorSession> findByTokenHash(String tokenHash);
}
