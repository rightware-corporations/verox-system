package com.rightware.verox.authentication.bootstrap;

import com.rightware.verox.authentication.application.ApiKeyService;
import com.rightware.verox.authentication.application.IssuedApiKey;
import com.rightware.verox.authentication.domain.ApiKeyEnvironment;
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
@ConditionalOnProperty(name = "verox.bootstrap.enabled", havingValue = "true")
public class BootstrapMerchantRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapMerchantRunner.class);

    private final MerchantRepository merchantRepository;
    private final ApiKeyService apiKeyService;
    private final String merchantName;
    private final ApiKeyEnvironment environment;

    public BootstrapMerchantRunner(
        MerchantRepository merchantRepository,
        ApiKeyService apiKeyService,
        @Value("${verox.bootstrap.merchant-name:VEROX MVP Merchant}") String merchantName,
        @Value("${verox.bootstrap.environment:TEST}") ApiKeyEnvironment environment
    ) {
        this.merchantRepository = merchantRepository;
        this.apiKeyService = apiKeyService;
        this.merchantName = merchantName;
        this.environment = environment;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (merchantRepository.findByNameIgnoreCase(merchantName).isPresent()) {
            log.info("VEROX bootstrap skipped: merchant '{}' already exists.", merchantName);
            return;
        }

        Merchant merchant = merchantRepository.save(new Merchant(merchantName));
        IssuedApiKey apiKey = apiKeyService.issue(merchant, environment);

        log.warn("VEROX bootstrap merchant created: {} ({})", merchant.getName(), merchant.getId());
        log.warn("VEROX bootstrap API key — copy and store securely; it will not be shown again: {}", apiKey.value());
        log.warn("Disable VEROX_BOOTSTRAP_ENABLED after provisioning the merchant.");
    }
}
