package com.rightware.verox.authentication.application;

import com.rightware.verox.authentication.domain.ApiKey;
import com.rightware.verox.authentication.domain.ApiKeyEnvironment;
import com.rightware.verox.authentication.domain.ApiKeyStatus;
import com.rightware.verox.authentication.repository.ApiKeyRepository;
import com.rightware.verox.merchant.domain.Merchant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

@Service
public class ApiKeyService {

    private static final int SECRET_BYTES = 32;
    private static final int STORED_PREFIX_LENGTH = 20;

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyHasher apiKeyHasher;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApiKeyService(ApiKeyRepository apiKeyRepository, ApiKeyHasher apiKeyHasher) {
        this.apiKeyRepository = apiKeyRepository;
        this.apiKeyHasher = apiKeyHasher;
    }

    @Transactional
    public IssuedApiKey issue(Merchant merchant, ApiKeyEnvironment environment) {
        byte[] secret = new byte[SECRET_BYTES];
        secureRandom.nextBytes(secret);

        String token = environment.tokenPrefix()
            + Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        String prefix = token.substring(0, Math.min(STORED_PREFIX_LENGTH, token.length()));
        String hash = apiKeyHasher.hash(token);

        ApiKey apiKey = apiKeyRepository.save(new ApiKey(merchant, prefix, hash, environment));
        return new IssuedApiKey(apiKey.getId(), token, prefix, environment);
    }

    @Transactional(readOnly = true)
    public Optional<MerchantPrincipal> authenticate(String rawApiKey) {
        String hash = apiKeyHasher.hash(rawApiKey);

        return apiKeyRepository.findByKeyHashAndStatus(hash, ApiKeyStatus.ACTIVE)
            .filter(ApiKey::isActive)
            .filter(apiKey -> apiKey.getMerchant().isActive())
            .map(apiKey -> new MerchantPrincipal(
                apiKey.getMerchant().getId(),
                apiKey.getMerchant().getName(),
                apiKey.getId(),
                apiKey.getEnvironment()
            ));
    }
}
