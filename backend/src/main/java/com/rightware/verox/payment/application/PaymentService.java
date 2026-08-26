package com.rightware.verox.payment.application;

import org.springframework.beans.factory.annotation.Autowired;
import com.rightware.verox.authentication.application.MerchantPrincipal;
import com.rightware.verox.common.money.MoneyConverter;
import com.rightware.verox.common.web.ApiException;
import com.rightware.verox.payment.domain.Payment;
import com.rightware.verox.payment.domain.PaymentStatus;
import com.rightware.verox.payment.repository.PaymentRepository;
import com.rightware.verox.pilot.repository.PilotManualPaymentAcceptanceRepository;
import com.rightware.verox.pilot.repository.PilotManualPaymentRejectionRepository;
import com.rightware.verox.evidence.domain.Evidence;
import com.rightware.verox.evidence.repository.EvidenceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MoneyConverter moneyConverter;
    private final PilotManualPaymentAcceptanceRepository manualAcceptanceRepository;
    private final PilotManualPaymentRejectionRepository manualRejectionRepository;
    private final EvidenceRepository evidenceRepository;

    @Autowired
    public PaymentService(
        PaymentRepository paymentRepository,
        MoneyConverter moneyConverter,
        PilotManualPaymentAcceptanceRepository manualAcceptanceRepository,
        PilotManualPaymentRejectionRepository manualRejectionRepository,
        EvidenceRepository evidenceRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.moneyConverter = moneyConverter;
        this.manualAcceptanceRepository = manualAcceptanceRepository;
        this.manualRejectionRepository = manualRejectionRepository;
        this.evidenceRepository = evidenceRepository;
    }

    public PaymentService(PaymentRepository paymentRepository, MoneyConverter moneyConverter, PilotManualPaymentAcceptanceRepository manualAcceptanceRepository) {
        this(paymentRepository, moneyConverter, manualAcceptanceRepository, null, null);
    }

    @Transactional(readOnly = true)
    public PaymentView getForMerchant(MerchantPrincipal principal, String publicId) {
        Payment payment = paymentRepository
            .findByPublicIdAndMerchantId(publicId, principal.merchantId())
            .orElseThrow(() -> paymentNotFound());

        if (payment.getEnvironment() != principal.environment()) {
            throw paymentNotFound();
        }

        var manualAcceptance = manualAcceptanceRepository
            .findByPaymentIdAndMerchantId(payment.getId(), principal.merchantId())
            .orElse(null);

        var manualRejection = manualRejectionRepository == null ? null : manualRejectionRepository.findByPaymentIdAndMerchantId(payment.getId(), principal.merchantId()).orElse(null);
        String effectiveStatus = payment.getStatus() == PaymentStatus.CONFIRMED
            ? PaymentStatus.CONFIRMED.name()
            : manualAcceptance != null
                ? "MANUALLY_ACCEPTED"
                : manualRejection != null
                    ? "MANUALLY_REJECTED"
                    : payment.getStatus().name();
        Evidence evidence = evidenceRepository == null ? null : evidenceRepository.findAllByPaymentIdOrderByReceivedAtAsc(payment.getId()).stream()
            .filter(item -> item.getOrigin().name().equals("CUSTOMER") && item.getRawContent() != null)
            .reduce((first, second) -> second).orElse(null);
        String displayProvider = payment.getProvider() != null
            ? payment.getProvider()
            : evidence == null ? null : evidence.getProvider();
        PaymentView.CustomerEvidenceView customerEvidence = evidence == null ? null : new PaymentView.CustomerEvidenceView(
            evidence.getProvider(),
            moneyConverter.toMajorString(payment.getAmountMinor()),
            payment.getCheckoutSession().getExternalReference(),
            evidence.getReceivedAt(),
            evidence.getRawContent()
        );

        return new PaymentView(
            payment.getPublicId(),
            payment.getCheckoutSession().getPublicId(),
            payment.getCheckoutSession().getExternalReference(),
            payment.getStatus().name(),
            effectiveStatus,
            moneyConverter.toMajorString(payment.getAmountMinor()),
            payment.getCurrency(),
            displayProvider,
            payment.getConfirmedAt(),
            manualAcceptance == null ? null : manualAcceptance.getAcceptedAt(),
            manualRejection == null ? null : manualRejection.getRejectedAt(),
            manualAcceptance != null ? manualAcceptance.getReason() : manualRejection == null ? null : manualRejection.getReason(),
            customerEvidence
        );
    }

    private ApiException paymentNotFound() {
        return new ApiException(
            HttpStatus.NOT_FOUND,
            "PAYMENT_NOT_FOUND",
            "Payment was not found."
        );
    }
}
