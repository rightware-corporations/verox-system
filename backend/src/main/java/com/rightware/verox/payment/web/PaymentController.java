package com.rightware.verox.payment.web;

import com.rightware.verox.authentication.application.MerchantPrincipal;
import com.rightware.verox.payment.application.PaymentService;
import com.rightware.verox.payment.application.PaymentView;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/{paymentId}")
    public PaymentView get(
        @AuthenticationPrincipal MerchantPrincipal principal,
        @PathVariable String paymentId
    ) {
        return paymentService.getForMerchant(principal.merchantId(), paymentId);
    }
}
