package com.rightware.verox.webhook.application;

import com.rightware.verox.common.id.ResourceIdGenerator;
import com.rightware.verox.common.web.ApiException;
import com.rightware.verox.merchant.domain.Merchant;
import com.rightware.verox.merchant.repository.MerchantRepository;
import com.rightware.verox.webhook.domain.WebhookEndpoint;
import com.rightware.verox.webhook.repository.WebhookEndpointRepository;
import com.rightware.verox.webhook.security.WebhookDestinationPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class WebhookEndpointService {

    private final MerchantRepository merchantRepository;
    private final WebhookEndpointRepository endpointRepository;
    private final ResourceIdGenerator resourceIdGenerator;
    private final WebhookSignatureService signatureService;
    private final WebhookDestinationPolicy destinationPolicy;

    public WebhookEndpointService(
        MerchantRepository merchantRepository,
        WebhookEndpointRepository endpointRepository,
        ResourceIdGenerator resourceIdGenerator,
        WebhookSignatureService signatureService,
        WebhookDestinationPolicy destinationPolicy
    ) {
        this.merchantRepository = merchantRepository;
        this.endpointRepository = endpointRepository;
        this.resourceIdGenerator = resourceIdGenerator;
        this.signatureService = signatureService;
        this.destinationPolicy = destinationPolicy;
    }

    @Transactional
    public WebhookEndpointView configure(UUID merchantId, String url) {
        String normalizedUrl;
        try {
            normalizedUrl = destinationPolicy.validate(url);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "INVALID_WEBHOOK_URL",
                exception.getMessage()
            );
        }

        Merchant merchant = merchantRepository.findById(merchantId)
            .filter(Merchant::isActive)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "MERCHANT_NOT_FOUND",
                "Merchant was not found."
            ));

        WebhookEndpoint endpoint = endpointRepository.findByMerchantId(merchantId)
            .map(existing -> {
                existing.updateUrl(normalizedUrl);
                return existing;
            })
            .orElseGet(() -> new WebhookEndpoint(
                resourceIdGenerator.generate("whep"),
                merchant,
                normalizedUrl
            ));

        endpointRepository.save(endpoint);

        return new WebhookEndpointView(
            endpoint.getPublicId(),
            endpoint.getUrl(),
            endpoint.getStatus().name(),
            signatureService.signingSecret(endpoint.getPublicId())
        );
    }
}