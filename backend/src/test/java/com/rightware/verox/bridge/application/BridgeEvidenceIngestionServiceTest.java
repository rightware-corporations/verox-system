package com.rightware.verox.bridge.application;

import com.rightware.verox.bridge.domain.Bridge;
import com.rightware.verox.bridge.domain.BridgeStatus;
import com.rightware.verox.bridge.repository.BridgeRepository;
import com.rightware.verox.common.web.ApiException;
import com.rightware.verox.evidence.application.EvidenceService;
import com.rightware.verox.evidence.domain.Evidence;
import com.rightware.verox.evidence.domain.EvidenceIngestSource;
import com.rightware.verox.evidence.domain.EvidenceKind;
import com.rightware.verox.evidence.domain.EvidenceOrigin;
import com.rightware.verox.merchant.domain.Merchant;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BridgeEvidenceIngestionServiceTest {

    @Test
    void ingestsRawProviderSmsIntoEvidenceInfrastructure() {
        BridgeRepository bridgeRepository = mock(BridgeRepository.class);
        EvidenceService evidenceService = mock(EvidenceService.class);
        Merchant merchant = new Merchant("Event Merchant");
        Bridge bridge = new Bridge("brg_test123", merchant, "Receiving iPhone", "MPESA");
        BridgePrincipal principal = new BridgePrincipal(
            bridge.getId(),
            bridge.getPublicId(),
            merchant.getId(),
            bridge.getProvider()
        );
        Instant receivedAt = Instant.parse("2026-08-09T14:00:00Z");
        Evidence evidence = new Evidence(
            "ev_test123",
            merchant,
            null,
            EvidenceOrigin.PROVIDER,
            EvidenceKind.SMS,
            EvidenceIngestSource.VEROX_BRIDGE,
            "MPESA",
            "a".repeat(64),
            "text/plain",
            null,
            null,
            "M-Pesa official SMS",
            null,
            receivedAt
        );

        when(bridgeRepository.findByIdAndStatus(bridge.getId(), BridgeStatus.ACTIVE))
            .thenReturn(Optional.of(bridge));
        when(evidenceService.registerProviderRaw(
            eq(merchant),
            eq(EvidenceKind.SMS),
            eq(EvidenceIngestSource.VEROX_BRIDGE),
            eq("MPESA"),
            eq("M-Pesa official SMS"),
            eq(null),
            eq(receivedAt)
        )).thenReturn(evidence);

        BridgeEvidenceIngestionService service = new BridgeEvidenceIngestionService(bridgeRepository, evidenceService);
        BridgeEvidenceView result = service.ingest(
            principal,
            "brg_test123",
            "M-Pesa official SMS",
            receivedAt
        );

        assertThat(result.id()).isEqualTo("ev_test123");
        assertThat(result.bridgeId()).isEqualTo("brg_test123");
        assertThat(result.origin()).isEqualTo("PROVIDER");
        assertThat(result.kind()).isEqualTo("SMS");
        assertThat(result.provider()).isEqualTo("MPESA");
        verify(evidenceService).registerProviderRaw(
            merchant,
            EvidenceKind.SMS,
            EvidenceIngestSource.VEROX_BRIDGE,
            "MPESA",
            "M-Pesa official SMS",
            null,
            receivedAt
        );
    }

    @Test
    void rejectsBridgeCredentialUsedForDifferentBridgePath() {
        BridgeRepository bridgeRepository = mock(BridgeRepository.class);
        EvidenceService evidenceService = mock(EvidenceService.class);
        Merchant merchant = new Merchant("Event Merchant");
        Bridge bridge = new Bridge("brg_test123", merchant, "Receiving iPhone", "MPESA");
        BridgePrincipal principal = new BridgePrincipal(
            bridge.getId(),
            bridge.getPublicId(),
            merchant.getId(),
            bridge.getProvider()
        );

        BridgeEvidenceIngestionService service = new BridgeEvidenceIngestionService(bridgeRepository, evidenceService);

        assertThatThrownBy(() -> service.ingest(
            principal,
            "brg_other",
            "M-Pesa official SMS",
            Instant.now()
        ))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("Bridge credential");

        verify(bridgeRepository, never()).findByIdAndStatus(any(), any());
        verify(evidenceService, never()).registerProviderRaw(any(), any(), any(), any(), any(), any(), any());
    }
}
