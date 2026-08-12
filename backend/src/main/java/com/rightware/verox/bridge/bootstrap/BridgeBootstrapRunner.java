package com.rightware.verox.bridge.bootstrap;

import com.rightware.verox.authentication.domain.ApiKeyEnvironment;
import com.rightware.verox.bridge.application.BridgeService;
import com.rightware.verox.bridge.application.IssuedBridgeCredential;
import com.rightware.verox.bridge.repository.BridgeRepository;
import com.rightware.verox.merchant.domain.Merchant;
import com.rightware.verox.merchant.repository.MerchantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "verox.bridge.bootstrap.enabled", havingValue = "true")
public class BridgeBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BridgeBootstrapRunner.class);

    private final MerchantRepository merchantRepository;
    private final BridgeRepository bridgeRepository;
    private final BridgeService bridgeService;
    private final String merchantName;
    private final ApiKeyEnvironment environment;
    private final String bridgeName;
    private final String provider;

    public BridgeBootstrapRunner(
        MerchantRepository merchantRepository,
        BridgeRepository bridgeRepository,
        BridgeService bridgeService,
        @Value("${verox.bridge.bootstrap.merchant-name:VEROX MVP Merchant}") String merchantName,
        @Value("${verox.bridge.bootstrap.environment:TEST}") ApiKeyEnvironment environment,
        @Value("${verox.bridge.bootstrap.name:M-Pesa Receiving Bridge}") String bridgeName,
        @Value("${verox.bridge.bootstrap.provider:MPESA}") String provider
    ) {
        this.merchantRepository = merchantRepository;
        this.bridgeRepository = bridgeRepository;
        this.bridgeService = bridgeService;
        this.merchantName = merchantName;
        this.environment = environment;
        this.bridgeName = bridgeName;
        this.provider = provider;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Merchant merchant = merchantRepository.findByNameIgnoreCase(merchantName)
            .filter(Merchant::isActive)
            .orElseThrow(() -> new IllegalStateException(
                "VEROX Bridge bootstrap merchant was not found or is inactive: " + merchantName
            ));

        if (bridgeRepository.findByMerchantIdAndNameIgnoreCase(merchant.getId(), bridgeName).isPresent()) {
            log.info("VEROX Bridge bootstrap skipped: bridge '{}' already exists for merchant '{}'.", bridgeName, merchantName);
            return;
        }

        IssuedBridgeCredential issued = bridgeService.provision(merchant, environment, bridgeName, provider);
        log.warn("VEROX Bridge created: {} ({}) [{}]", bridgeName, issued.bridgePublicId(), environment);
        log.warn("VEROX Bridge credential — copy and store securely; it will not be shown again: {}", issued.value());
        log.warn("Disable VEROX_BRIDGE_BOOTSTRAP_ENABLED after provisioning the bridge.");
    }
}
