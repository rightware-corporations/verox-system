package com.rightware.verox.checkout.application;

import com.rightware.verox.checkout.domain.CheckoutSession;
import com.rightware.verox.checkout.repository.CheckoutSessionRepository;
import com.rightware.verox.common.money.MoneyConverter;
import com.rightware.verox.common.web.ApiException;
import com.rightware.verox.payment.domain.Payment;
import com.rightware.verox.payment.domain.PaymentStatus;
import com.rightware.verox.payment.repository.PaymentRepository;
import com.rightware.verox.paymentchannel.application.PaymentChannelService;
import com.rightware.verox.evidence.domain.Evidence;
import com.rightware.verox.evidence.repository.EvidenceRepository;
import com.rightware.verox.pilot.repository.PilotManualPaymentAcceptanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HostedCheckoutBootstrapService {
    private final CheckoutSessionRepository checkoutSessionRepository;
    private final PaymentRepository paymentRepository;
    private final PilotManualPaymentAcceptanceRepository manualAcceptanceRepository;
    private final CheckoutSubmissionCapabilityService capabilityService;
    private final MoneyConverter moneyConverter;
    private final PaymentChannelService paymentChannelService;
    private final EvidenceRepository evidenceRepository;

    @Autowired
    public HostedCheckoutBootstrapService(
        CheckoutSessionRepository checkoutSessionRepository,
        PaymentRepository paymentRepository,
        PilotManualPaymentAcceptanceRepository manualAcceptanceRepository,
        CheckoutSubmissionCapabilityService capabilityService,
        MoneyConverter moneyConverter,
        PaymentChannelService paymentChannelService,
        EvidenceRepository evidenceRepository
    ) {
        this.checkoutSessionRepository = checkoutSessionRepository;
        this.paymentRepository = paymentRepository;
        this.manualAcceptanceRepository = manualAcceptanceRepository;
        this.capabilityService = capabilityService;
        this.moneyConverter = moneyConverter;
        this.paymentChannelService = paymentChannelService;
        this.evidenceRepository = evidenceRepository;
    }

    public HostedCheckoutBootstrapService(CheckoutSessionRepository checkoutSessionRepository, PaymentRepository paymentRepository, PilotManualPaymentAcceptanceRepository manualAcceptanceRepository, CheckoutSubmissionCapabilityService capabilityService, MoneyConverter moneyConverter, PaymentChannelService paymentChannelService) {
        this(checkoutSessionRepository, paymentRepository, manualAcceptanceRepository, capabilityService, moneyConverter, paymentChannelService, null);
    }

    @Transactional(readOnly = true)
    public HostedCheckoutBootstrapView get(String checkoutSessionId, String checkoutCapability) {
        CheckoutSession session = checkoutSessionRepository.findByPublicId(checkoutSessionId)
            .orElseThrow(this::checkoutNotFound);

        if (!session.getMerchant().isActive() || !capabilityService.matches(session, checkoutCapability)) {
            throw checkoutNotFound();
        }

        Payment payment = paymentRepository.findByCheckoutSessionId(session.getId())
            .orElseThrow(() -> new ApiException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "PAYMENT_STATE_ERROR",
                "Checkout Session exists without its Payment."
            ));

        var manualAcceptance = manualAcceptanceRepository
            .findByPaymentIdAndMerchantId(payment.getId(), session.getMerchant().getId())
            .orElse(null);

        String effectivePaymentStatus = payment.getStatus() == PaymentStatus.CONFIRMED
            ? PaymentStatus.CONFIRMED.name()
            : manualAcceptance != null ? "MANUALLY_ACCEPTED" : payment.getStatus().name();

        Evidence evidence = evidenceRepository == null ? null : evidenceRepository.findAllByPaymentIdOrderByReceivedAtAsc(payment.getId()).stream().filter(item -> item.getOrigin().name().equals("CUSTOMER") && item.getRawContent() != null).reduce((first, second) -> second).orElse(null);

        var channels = paymentChannelService
            .listActiveForCheckout(session.getMerchant().getId(), session.getEnvironment())
            .stream()
            .map(channel -> new HostedCheckoutBootstrapView.PaymentChannelView(
                channel.provider(),
                channel.displayName(),
                channel.kind(),
                true,
                channel.recipientDisplay(),
                channel.recipientName(),
                channel.instructions()
            ))
            .toList();

        return new HostedCheckoutBootstrapView(
            session.getPublicId(),
            payment.getPublicId(),
            session.getMerchant().getName(),
            session.getExternalReference(),
            session.getDescription(),
            moneyConverter.toMajorString(session.getAmountMinor()),
            session.getCurrency(),
            session.getStatus().name(),
            payment.getStatus().name(),
            effectivePaymentStatus,
            session.getExpiresAt(),
            session.getSuccessUrl(),
            session.getCancelUrl(),
            channels,
            evidence != null,
            evidence == null ? null : evidence.getReceivedAt()
        );
    }

    private ApiException checkoutNotFound() {
        return new ApiException(
            HttpStatus.NOT_FOUND,
            "CHECKOUT_SESSION_NOT_FOUND",
            "Checkout Session was not found."
        );
    }
}
