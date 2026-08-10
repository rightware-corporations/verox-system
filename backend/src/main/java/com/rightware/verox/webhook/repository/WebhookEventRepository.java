package com.rightware.verox.webhook.repository;

import com.rightware.verox.webhook.domain.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {
    Optional<WebhookEvent> findByMerchantIdAndTypeAndAggregateTypeAndAggregatePublicId(
        UUID merchantId,
        String type,
        String aggregateType,
        String aggregatePublicId
    );
}
