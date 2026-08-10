package com.rightware.verox.webhook.repository;

import com.rightware.verox.webhook.domain.WebhookDelivery;
import com.rightware.verox.webhook.domain.WebhookDeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {
    Optional<WebhookDelivery> findByEventIdAndEndpointId(UUID eventId, UUID endpointId);

    List<WebhookDelivery> findTop20ByStatusInAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
        Collection<WebhookDeliveryStatus> statuses,
        Instant now
    );
}
