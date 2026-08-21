package com.rightware.verox.payment.application;

import com.rightware.verox.authentication.application.MerchantPrincipal;
import com.rightware.verox.common.money.MoneyConverter;
import com.rightware.verox.common.web.ApiException;
import com.rightware.verox.payment.domain.Payment;
import com.rightware.verox.payment.domain.PaymentStatus;
import com.rightware.verox.payment.repository.PaymentRepository;
import com.rightware.verox.pilot.repository.PilotManualPaymentAcceptanceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MoneyConverter moneyConverter;
    private final PilotManualPaymentAcceptanceRepository manualAcceptanceRepository;

    public PaymentService(
        PaymentRepository paymentRepository,
        MoneyConverter moneyConverter,
        PilotManualPaymentAcceptanceRepository manualAcceptanceRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.moneyConverter = moneyConverter;
        this.manualAcceptanceRepository = manualAcceptanceRepository;
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

        String effectiveStatus = payment.getStatus() == PaymentStatus.CONFIRMED
            ? PaymentStatus.CONFIRMED.name()
            : manualAcceptance != null
                ? "MANUALLY_ACCEPTED"
                : payment.getStatus().name();

        return new PaymentView(
            payment.getPublicId(),
            payment.getCheckoutSession().getPublicId(),
            payment.getCheckoutSession().getExternalReference(),
            payment.getStatus().name(),
            effectiveStatus,
            moneyConverter.toMajorString(payment.getAmountMinor()),
            payment.getCurrency(),
            payment.getProvider(),
            payment.getConfirmedAt(),
            manualAcceptance == null ? null : manualAcceptance.getAcceptedAt()
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
