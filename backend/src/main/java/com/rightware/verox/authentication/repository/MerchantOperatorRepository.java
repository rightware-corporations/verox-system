package com.rightware.verox.authentication.repository;

import com.rightware.verox.authentication.domain.MerchantOperator;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MerchantOperatorRepository extends JpaRepository<MerchantOperator, UUID> {
    @EntityGraph(attributePaths = "merchant")
    Optional<MerchantOperator> findByUsername(String username);
}
