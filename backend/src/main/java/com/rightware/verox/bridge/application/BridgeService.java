package com.rightware.verox.bridge.application;

import com.rightware.verox.authentication.domain.ApiKeyEnvironment;
import com.rightware.verox.bridge.domain.Bridge;
import com.rightware.verox.bridge.domain.BridgeCredential;
import com.rightware.verox.bridge.domain.BridgeCredentialStatus;
import com.rightware.verox.bridge.repository.BridgeCredentialRepository;
import com.rightware.verox.bridge.repository.BridgeRepository;
import com.rightware.verox.common.id.ResourceIdGenerator;
import com.rightware.verox.merchant.domain.Merchant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Service
public class BridgeService {

    private static final int SECRET_BYTES = 32;
    private static final int STORED_PREFIX_LENGTH = 20;
    private static final String TOKEN_PREFIX = "vx_bridge_";

    private final BridgeRepository bridgeRepository;
    private final BridgeCredentialRepository credentialRepository;
    private final BridgeCredentialHasher credentialHasher;
    private final ResourceIdGenerator resourceIdGenerator;
    private final SecureRandom secureRandom = new SecureRandom();

    public BridgeService(
        BridgeRepository bridgeRepository,
        BridgeCredentialRepository credentialRepository,
        BridgeCredentialHasher credentialHasher,
        ResourceIdGenerator resourceIdGenerator
    ) {
        this.bridgeRepository = bridgeRepository;
        this.credentialRepository = credentialRepository;
        this.credentialHasher = credentialHasher;
        this.resourceIdGenerator = resourceIdGenerator;
    }

    @Transactional
    public IssuedBridgeCredential provision(
        Merchant merchant,
        ApiKeyEnvironment environment,
        String name,
        String provider
    ) {
        Bridge bridge = bridgeRepository.save(new Bridge(
            resourceIdGenerator.generate("brg"),
            merchant,
            environment,
            name,
            provider
        ));

        byte[] secret = new byte[SECRET_BYTES];
        secureRandom.nextBytes(secret);
        String token = TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        String prefix = token.substring(0, Math.min(STORED_PREFIX_LENGTH, token.length()));
        String hash = credentialHasher.hash(token);

        BridgeCredential credential = credentialRepository.save(new BridgeCredential(bridge, prefix, hash));
        return new IssuedBridgeCredential(
            bridge.getId(),
            bridge.getPublicId(),
            credential.getId(),
            token,
            prefix
        );
    }

    @Transactional
    public Optional<BridgePrincipal> authenticate(String rawCredential) {
        if (rawCredential == null || !rawCredential.startsWith(TOKEN_PREFIX)) {
            return Optional.empty();
        }

        String hash = credentialHasher.hash(rawCredential);
        Optional<BridgeCredential> match = credentialRepository.findByKeyHashAndStatus(
            hash,
            BridgeCredentialStatus.ACTIVE
        );

        if (match.isEmpty()) {
            return Optional.empty();
        }

        BridgeCredential credential = match.orElseThrow();
        Bridge bridge = credential.getBridge();
        if (!credential.isActive() || !bridge.isActive() || !bridge.getMerchant().isActive()) {
            return Optional.empty();
        }

        credential.markUsed(Instant.now());
        credentialRepository.save(credential);

        return Optional.of(new BridgePrincipal(
            bridge.getId(),
            bridge.getPublicId(),
            bridge.getMerchant().getId(),
            bridge.getEnvironment(),
            bridge.getProvider()
        ));
    }
}
