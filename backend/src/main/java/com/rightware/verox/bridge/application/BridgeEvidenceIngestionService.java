package com.rightware.verox.bridge.application;

import com.rightware.verox.bridge.domain.Bridge;
import com.rightware.verox.bridge.domain.BridgeStatus;
import com.rightware.verox.bridge.repository.BridgeRepository;
import com.rightware.verox.common.web.ApiException;
import com.rightware.verox.evidence.application.EvidenceIngestedEvent;
import com.rightware.verox.evidence.application.EvidenceService;
import com.rightware.verox.evidence.domain.Evidence;
import com.rightware.verox.evidence.domain.EvidenceIngestSource;
import com.rightware.verox.evidence.domain.EvidenceKind;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class BridgeEvidenceIngestionService {

    private final BridgeRepository bridgeRepository;
    private final EvidenceService evidenceService;
    private final ApplicationEventPublisher eventPublisher;

    public BridgeEvidenceIngestionService(
        BridgeRepository bridgeRepository,
        EvidenceService evidenceService,
        ApplicationEventPublisher eventPublisher
    ) {
        this.bridgeRepository = bridgeRepository;
        this.evidenceService = evidenceService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public BridgeEvidenceView ingest(BridgePrincipal principal, String pathBridgeId, String content, Instant receivedAt) {
        if (!principal.bridgePublicId().equals(pathBridgeId)) {
            throw new ApiException(
                HttpStatus.FORBIDDEN,
                "BRIDGE_SCOPE_VIOLATION",
                "Bridge credential cannot be used for this bridge."
            );
        }

        Bridge bridge = bridgeRepository.findByIdAndStatus(principal.bridgeId(), BridgeStatus.ACTIVE)
            .filter(candidate -> candidate.getPublicId().equals(principal.bridgePublicId()))
            .filter(candidate -> candidate.getMerchant().getId().equals(principal.merchantId()))
            .orElseThrow(() -> new ApiException(
                HttpStatus.UNAUTHORIZED,
                "BRIDGE_UNAVAILABLE",
                "Bridge is not available."
            ));

        Instant effectiveReceivedAt = receivedAt == null ? Instant.now() : receivedAt;
        Evidence evidence = evidenceService.registerProviderRaw(
            bridge.getMerchant(),
            EvidenceKind.SMS,
            EvidenceIngestSource.VEROX_BRIDGE,
            bridge.getProvider(),
            content,
            null,
            effectiveReceivedAt
        );

        eventPublisher.publishEvent(new EvidenceIngestedEvent(
            bridge.getMerchant().getId(),
            null,
            evidence.getOrigin(),
            evidence.getPublicId()
        ));

        return new BridgeEvidenceView(
            evidence.getPublicId(),
            bridge.getPublicId(),
            evidence.getOrigin().name(),
            evidence.getKind().name(),
            evidence.getProvider(),
            evidence.getReceivedAt()
        );
    }
}
