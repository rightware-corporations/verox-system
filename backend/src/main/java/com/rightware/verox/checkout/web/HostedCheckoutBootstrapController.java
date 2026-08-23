package com.rightware.verox.checkout.web;

import com.rightware.verox.checkout.application.HostedCheckoutBootstrapService;
import com.rightware.verox.checkout.application.HostedCheckoutBootstrapView;
import com.rightware.verox.evidence.web.CustomerMessageEvidenceController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/v1/checkout")
public class HostedCheckoutBootstrapController {

    private final HostedCheckoutBootstrapService bootstrapService;

    public HostedCheckoutBootstrapController(HostedCheckoutBootstrapService bootstrapService) {
        this.bootstrapService = bootstrapService;
    }

    @GetMapping("/{checkoutSessionId}")
    public HostedCheckoutBootstrapView get(
        @PathVariable String checkoutSessionId,
        @RequestHeader(
            value = CustomerMessageEvidenceController.CHECKOUT_CAPABILITY_HEADER,
            required = false
        ) String checkoutCapability
    ) {
        return bootstrapService.get(checkoutSessionId, checkoutCapability);
    }
}
