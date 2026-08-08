package com.rightware.verox.checkout.web;

import com.rightware.verox.authentication.application.MerchantPrincipal;
import com.rightware.verox.checkout.application.CheckoutSessionService;
import com.rightware.verox.checkout.application.CheckoutSessionView;
import com.rightware.verox.checkout.application.CreateCheckoutSessionCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/v1/checkout/sessions")
public class CheckoutSessionController {

    private final CheckoutSessionService checkoutSessionService;

    public CheckoutSessionController(CheckoutSessionService checkoutSessionService) {
        this.checkoutSessionService = checkoutSessionService;
    }

    @PostMapping
    public ResponseEntity<CheckoutSessionView> create(
        @AuthenticationPrincipal MerchantPrincipal principal,
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @Valid @RequestBody CreateCheckoutSessionRequest request
    ) {
        CheckoutSessionView result = checkoutSessionService.create(new CreateCheckoutSessionCommand(
            principal.merchantId(),
            principal.environment(),
            idempotencyKey,
            request.amount(),
            request.currency(),
            request.externalReference(),
            request.description(),
            request.successUrl(),
            request.cancelUrl()
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/{checkoutSessionId}")
    public CheckoutSessionView get(
        @AuthenticationPrincipal MerchantPrincipal principal,
        @PathVariable String checkoutSessionId
    ) {
        return checkoutSessionService.getForMerchant(principal.merchantId(), checkoutSessionId);
    }

    public record CreateCheckoutSessionRequest(
        @NotNull BigDecimal amount,
        @NotBlank @Size(max = 3) String currency,
        @NotBlank @Size(max = 160) String externalReference,
        @Size(max = 255) String description,
        @NotBlank String successUrl,
        @NotBlank String cancelUrl
    ) {
    }
}
