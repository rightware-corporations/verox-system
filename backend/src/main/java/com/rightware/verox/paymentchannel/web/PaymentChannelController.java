package com.rightware.verox.paymentchannel.web;

import com.rightware.verox.authentication.application.MerchantPrincipal;
import com.rightware.verox.paymentchannel.application.PaymentChannelService;
import com.rightware.verox.paymentchannel.application.PaymentChannelView;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/payment-channels")
public class PaymentChannelController {

    private final PaymentChannelService paymentChannelService;

    public PaymentChannelController(PaymentChannelService paymentChannelService) {
        this.paymentChannelService = paymentChannelService;
    }

    @GetMapping
    public List<PaymentChannelView> list(@AuthenticationPrincipal MerchantPrincipal principal) {
        return paymentChannelService.listForMerchant(
            principal.merchantId(),
            principal.environment()
        );
    }
}
