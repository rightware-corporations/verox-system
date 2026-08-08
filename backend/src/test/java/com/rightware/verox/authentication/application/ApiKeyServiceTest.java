package com.rightware.verox.authentication.application;

import com.rightware.verox.authentication.domain.ApiKey;
import com.rightware.verox.authentication.domain.ApiKeyEnvironment;
import com.rightware.verox.authentication.domain.ApiKeyStatus;
import com.rightware.verox.authentication.repository.ApiKeyRepository;
import com.rightware.verox.merchant.domain.Merchant;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiKeyServiceTest {

    @Test
    void authenticatesAnActiveApiKeyForAnActiveMerchant() {
        ApiKeyRepository repository = mock(ApiKeyRepository.class);
        ApiKeyHasher hasher = mock(ApiKeyHasher.class);
        ApiKeyService service = new ApiKeyService(repository, hasher);

        Merchant merchant = new Merchant("Event Merchant");
        ApiKey apiKey = new ApiKey(merchant, "vx_live_123456789012", "stored-hash", ApiKeyEnvironment.LIVE);

        when(hasher.hash("vx_live_secret")).thenReturn("stored-hash");
        when(repository.findByKeyHashAndStatus("stored-hash", ApiKeyStatus.ACTIVE))
            .thenReturn(Optional.of(apiKey));

        Optional<MerchantPrincipal> principal = service.authenticate("vx_live_secret");

        assertThat(principal).isPresent();
        assertThat(principal.orElseThrow().merchantId()).isEqualTo(merchant.getId());
        assertThat(principal.orElseThrow().merchantName()).isEqualTo("Event Merchant");
        assertThat(principal.orElseThrow().environment()).isEqualTo(ApiKeyEnvironment.LIVE);
    }

    @Test
    void rejectsAnUnknownApiKey() {
        ApiKeyRepository repository = mock(ApiKeyRepository.class);
        ApiKeyHasher hasher = mock(ApiKeyHasher.class);
        ApiKeyService service = new ApiKeyService(repository, hasher);

        when(hasher.hash("vx_live_invalid")).thenReturn("unknown-hash");
        when(repository.findByKeyHashAndStatus("unknown-hash", ApiKeyStatus.ACTIVE))
            .thenReturn(Optional.empty());

        assertThat(service.authenticate("vx_live_invalid")).isEmpty();
    }
}
