package com.rightware.verox.webhook.web;

import com.rightware.verox.authentication.application.MerchantPrincipal;
import com.rightware.verox.webhook.application.WebhookEndpointService;
import com.rightware.verox.webhook.application.WebhookEndpointView;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/webhook-endpoint")
public class WebhookEndpointController {

    private final WebhookEndpointService webhookEndpointService;

    public WebhookEndpointController(WebhookEndpointService webhookEndpointService) {
        this.webhookEndpointService = webhookEndpointService;
    }

    @PutMapping
    public WebhookEndpointView configure(
        @AuthenticationPrincipal MerchantPrincipal principal,
        @Valid @RequestBody ConfigureWebhookEndpointRequest request
    ) {
        return webhookEndpointService.configure(principal.merchantId(), request.url());
    }
}
