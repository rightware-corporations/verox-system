package com.rightware.verox.merchant.repository;

import com.rightware.verox.merchant.domain.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MerchantRepository extends JpaRepository<Merchant, UUID> {
    Optional<Merchant> findByNameIgnoreCase(String name);
}
