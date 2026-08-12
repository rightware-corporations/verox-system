package com.rightware.verox.bridge.application;

import com.rightware.verox.authentication.domain.ApiKeyEnvironment;
import com.rightware.verox.bridge.domain.Bridge;
import com.rightware.verox.bridge.domain.BridgeCredential;
import com.rightware.verox.bridge.domain.BridgeCredentialStatus;
import com.rightware.verox.bridge.repository.BridgeCredentialRepository;
import com.rightware.verox.bridge.repository.BridgeRepository;
import com.rightware.verox.common.id.ResourceIdGenerator;
import com.rightware.verox.merchant.domain.Merchant;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BridgeServiceTest {

    @Test
    void provisionsDedicatedBridgeCredentialInExplicitEnvironment() {
        BridgeRepository bridgeRepository = mock(BridgeRepository.class);
        BridgeCredentialRepository credentialRepository = mock(BridgeCredentialRepository.class);
        BridgeCredentialHasher hasher = mock(BridgeCredentialHasher.class);
        ResourceIdGenerator ids = mock(ResourceIdGenerator.class);
        Merchant merchant = new Merchant("Event Merchant");

        when(ids.generate("brg")).thenReturn("brg_test123");
        when(bridgeRepository.save(any(Bridge.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(credentialRepository.save(any(BridgeCredential.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(hasher.hash(any())).thenReturn("a".repeat(64));

        BridgeService service = new BridgeService(bridgeRepository, credentialRepository, hasher, ids);
        IssuedBridgeCredential issued = service.provision(
            merchant,
            ApiKeyEnvironment.TEST,
            "Receiving iPhone",
            "MPESA"
        );

        assertThat(issued.bridgePublicId()).isEqualTo("brg_test123");
        assertThat(issued.value()).startsWith("vx_bridge_");
        assertThat(issued.prefix()).startsWith("vx_bridge_");
        verify(bridgeRepository).save(any(Bridge.class));
        verify(credentialRepository).save(any(BridgeCredential.class));
    }

    @Test
    void authenticatesActiveBridgeCredentialWithEnvironment() {
        BridgeRepository bridgeRepository = mock(BridgeRepository.class);
        BridgeCredentialRepository credentialRepository = mock(BridgeCredentialRepository.class);
        BridgeCredentialHasher hasher = mock(BridgeCredentialHasher.class);
        ResourceIdGenerator ids = mock(ResourceIdGenerator.class);
        Merchant merchant = new Merchant("Event Merchant");
        Bridge bridge = new Bridge(
            "brg_test123",
            merchant,
            ApiKeyEnvironment.LIVE,
            "Receiving iPhone",
            "MPESA"
        );
        BridgeCredential credential = new BridgeCredential(bridge, "vx_bridge_prefix", "b".repeat(64));

        when(hasher.hash("vx_bridge_secret")).thenReturn("b".repeat(64));
        when(credentialRepository.findByKeyHashAndStatus("b".repeat(64), BridgeCredentialStatus.ACTIVE))
            .thenReturn(Optional.of(credential));
        when(credentialRepository.save(any(BridgeCredential.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BridgeService service = new BridgeService(bridgeRepository, credentialRepository, hasher, ids);
        Optional<BridgePrincipal> result = service.authenticate("vx_bridge_secret");

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().bridgePublicId()).isEqualTo("brg_test123");
        assertThat(result.orElseThrow().merchantId()).isEqualTo(merchant.getId());
        assertThat(result.orElseThrow().environment()).isEqualTo(ApiKeyEnvironment.LIVE);
        assertThat(result.orElseThrow().provider()).isEqualTo("MPESA");
        verify(credentialRepository).save(credential);
    }
}
