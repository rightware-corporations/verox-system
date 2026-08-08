package com.rightware.verox.merchant.web;

import com.rightware.verox.authentication.application.MerchantPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/account")
public class AccountController {

    @GetMapping
    public AccountResponse getAccount(@AuthenticationPrincipal MerchantPrincipal principal) {
        return new AccountResponse(
            principal.merchantId(),
            principal.merchantName(),
            principal.environment().name()
        );
    }

    public record AccountResponse(
        UUID merchantId,
        String merchantName,
        String environment
    ) {
    }
}
