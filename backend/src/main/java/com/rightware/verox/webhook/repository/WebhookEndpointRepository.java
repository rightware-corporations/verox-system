package com.rightware.verox.webhook.repository;

import com.rightware.verox.webhook.domain.WebhookEndpoint;
import com.rightware.verox.webhook.domain.WebhookEndpointStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WebhookEndpointRepository extends JpaRepository<WebhookEndpoint, UUID> {

    @EntityGraph(attributePaths = "merchant")
    Optional<WebhookEndpoint> findByMerchantId(UUID merchantId);

    @EntityGraph(attributePaths = "merchant")
    Optional<WebhookEndpoint> findByMerchantIdAndStatus(UUID merchantId, WebhookEndpointStatus status);
}
