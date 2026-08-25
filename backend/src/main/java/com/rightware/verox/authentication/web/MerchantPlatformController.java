package com.rightware.verox.authentication.web;

import com.rightware.verox.authentication.application.MerchantOperatorPrincipal;
import com.rightware.verox.authentication.application.MerchantPrincipal;
import com.rightware.verox.payment.application.PaymentFeedService;
import com.rightware.verox.payment.application.PaymentPageView;
import com.rightware.verox.payment.application.PaymentService;
import com.rightware.verox.payment.application.PaymentView;
import com.rightware.verox.paymentchannel.application.PaymentChannelService;
import com.rightware.verox.paymentchannel.application.PaymentChannelView;
import com.rightware.verox.pilot.application.PilotManualPaymentAcceptanceService;
import com.rightware.verox.pilot.application.PilotManualPaymentAcceptanceView;
import com.rightware.verox.pilot.application.PilotManualPaymentRejectionService;
import com.rightware.verox.pilot.application.PilotManualPaymentRejectionView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/platform/v1")
public class MerchantPlatformController {
    private final PaymentFeedService paymentFeedService;
    private final PaymentService paymentService;
    private final PaymentChannelService paymentChannelService;
    private final PilotManualPaymentAcceptanceService manualAcceptanceService;
    private final PilotManualPaymentRejectionService manualRejectionService;

    public MerchantPlatformController(
        PaymentFeedService paymentFeedService,
        PaymentService paymentService,
        PaymentChannelService paymentChannelService,
        PilotManualPaymentAcceptanceService manualAcceptanceService,
        PilotManualPaymentRejectionService manualRejectionService
    ) {
        this.paymentFeedService = paymentFeedService;
        this.paymentService = paymentService;
        this.paymentChannelService = paymentChannelService;
        this.manualAcceptanceService = manualAcceptanceService;
        this.manualRejectionService = manualRejectionService;
    }

    @GetMapping("/account")
    public AccountView account(@AuthenticationPrincipal MerchantOperatorPrincipal principal) {
        return new AccountView(
            principal.operatorId().toString(),
            principal.operatorDisplayName(),
            principal.merchantId().toString(),
            principal.merchantName(),
            principal.environment().name()
        );
    }

    @GetMapping("/payments")
    public PaymentPageView payments(
        @AuthenticationPrincipal MerchantOperatorPrincipal principal,
        @RequestParam(required = false) String status,
        @RequestParam(name = "attention_required", required = false) Boolean attentionRequired,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return paymentFeedService.list(asMerchantPrincipal(principal), status, attentionRequired, page, size);
    }

    @GetMapping("/payments/{paymentId}")
    public PaymentView payment(
        @AuthenticationPrincipal MerchantOperatorPrincipal principal,
        @PathVariable String paymentId
    ) {
        return paymentService.getForMerchant(asMerchantPrincipal(principal), paymentId);
    }

    @PostMapping("/payments/{paymentId}/manual-acceptance")
    public PilotManualPaymentAcceptanceView acceptManually(
        @AuthenticationPrincipal MerchantOperatorPrincipal principal,
        @PathVariable String paymentId,
        @Valid @RequestBody ManualAcceptanceRequest request
    ) {
        return manualAcceptanceService.accept(principal, paymentId, request.reason());
    }

    @PostMapping("/payments/{paymentId}/manual-rejection")
    public PilotManualPaymentRejectionView rejectManually(
        @AuthenticationPrincipal MerchantOperatorPrincipal principal,
        @PathVariable String paymentId,
        @Valid @RequestBody ManualAcceptanceRequest request
    ) {
        return manualRejectionService.reject(principal, paymentId, request.reason());
    }

    @GetMapping("/payment-channels")
    public List<PaymentChannelView> paymentChannels(
        @AuthenticationPrincipal MerchantOperatorPrincipal principal
    ) {
        return paymentChannelService.listForMerchant(principal.merchantId(), principal.environment());
    }

    private MerchantPrincipal asMerchantPrincipal(MerchantOperatorPrincipal principal) {
        return new MerchantPrincipal(
            principal.merchantId(),
            principal.merchantName(),
            principal.sessionId(),
            principal.environment()
        );
    }

    public record ManualAcceptanceRequest(@Size(max = 255) String reason) {}

    public record AccountView(
        String operatorId,
        String operatorDisplayName,
        String merchantId,
        String merchantName,
        String environment
    ) {}
}
