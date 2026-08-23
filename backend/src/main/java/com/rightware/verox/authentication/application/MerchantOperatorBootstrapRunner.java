package com.rightware.verox.authentication.application;

import com.rightware.verox.authentication.domain.ApiKeyEnvironment;
import com.rightware.verox.authentication.domain.MerchantOperator;
import com.rightware.verox.authentication.repository.MerchantOperatorRepository;
import com.rightware.verox.merchant.repository.MerchantRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class MerchantOperatorBootstrapRunner implements CommandLineRunner {
    private final MerchantOperatorRepository operatorRepository;
    private final MerchantRepository merchantRepository;
    private final boolean enabled;
    private final String merchantName;
    private final ApiKeyEnvironment environment;
    private final String username;
    private final String displayName;
    private final String password;

    public MerchantOperatorBootstrapRunner(
        MerchantOperatorRepository operatorRepository,
        MerchantRepository merchantRepository,
        @Value("${verox.operator-bootstrap.enabled:false}") boolean enabled,
        @Value("${verox.operator-bootstrap.merchant-name:}") String merchantName,
        @Value("${verox.operator-bootstrap.environment:TEST}") ApiKeyEnvironment environment,
        @Value("${verox.operator-bootstrap.username:}") String username,
        @Value("${verox.operator-bootstrap.display-name:}") String displayName,
        @Value("${verox.operator-bootstrap.password:}") String password
    ) {
        this.operatorRepository = operatorRepository;
        this.merchantRepository = merchantRepository;
        this.enabled = enabled;
        this.merchantName = merchantName;
        this.environment = environment;
        this.username = username;
        this.displayName = displayName;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (!enabled) return;
        if (merchantName == null || merchantName.isBlank()
            || username == null || username.isBlank()
            || displayName == null || displayName.isBlank()
            || password == null || password.isBlank()) {
            throw new IllegalStateException("Operator bootstrap requires merchant name, username, display name and password");
        }

        String normalizedUsername = username.trim().toLowerCase(java.util.Locale.ROOT);
        if (operatorRepository.findByUsername(normalizedUsername).isPresent()) return;

        var merchant = merchantRepository.findByNameIgnoreCase(merchantName.trim())
            .orElseThrow(() -> new IllegalStateException("Operator bootstrap merchant was not found"));
        if (!merchant.isActive()) {
            throw new IllegalStateException("Operator bootstrap merchant must be active");
        }

        String passwordHash = new BCryptPasswordEncoder().encode(password);
        operatorRepository.save(new MerchantOperator(
            merchant,
            environment,
            normalizedUsername,
            displayName,
            passwordHash
        ));
    }
}
