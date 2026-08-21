package com.rightware.verox.pilot.web;

import com.rightware.verox.authentication.application.MerchantPrincipal;
import com.rightware.verox.pilot.application.PilotManualPaymentAcceptanceService;
import com.rightware.verox.pilot.application.PilotManualPaymentAcceptanceView;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/pilot/manual-acceptances")
public class PilotManualPaymentAcceptanceController {

    private final PilotManualPaymentAcceptanceService service;

    public PilotManualPaymentAcceptanceController(
        PilotManualPaymentAcceptanceService service
    ) {
        this.service = service;
    }

    @PostMapping("/{paymentId}")
    public PilotManualPaymentAcceptanceView accept(
        @AuthenticationPrincipal MerchantPrincipal principal,
        @PathVariable String paymentId,
        @Valid @RequestBody PilotManualPaymentAcceptanceRequest request
    ) {
        return service.accept(principal, paymentId, request.reason());
    }

    @GetMapping("/{paymentId}")
    public PilotManualPaymentAcceptanceView get(
        @AuthenticationPrincipal MerchantPrincipal principal,
        @PathVariable String paymentId
    ) {
        return service.get(principal, paymentId);
    }
}
