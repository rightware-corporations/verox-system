package com.rightware.verox.payment.web;

import com.rightware.verox.authentication.application.MerchantPrincipal;
import com.rightware.verox.payment.application.PaymentFeedService;
import com.rightware.verox.payment.application.PaymentPageView;
import com.rightware.verox.payment.application.PaymentService;
import com.rightware.verox.payment.application.PaymentView;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentFeedService paymentFeedService;

    public PaymentController(
        PaymentService paymentService,
        PaymentFeedService paymentFeedService
    ) {
        this.paymentService = paymentService;
        this.paymentFeedService = paymentFeedService;
    }

    @GetMapping
    public PaymentPageView list(
        @AuthenticationPrincipal MerchantPrincipal principal,
        @RequestParam(required = false) String status,
        @RequestParam(name = "attention_required", required = false) Boolean attentionRequired,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return paymentFeedService.list(
            principal,
            status,
            attentionRequired,
            page,
            size
        );
    }

    @GetMapping("/{paymentId}")
    public PaymentView get(
        @AuthenticationPrincipal MerchantPrincipal principal,
        @PathVariable String paymentId
    ) {
        return paymentService.getForMerchant(principal, paymentId);
    }
}
