package com.rightware.verox.pilot.application;

import com.rightware.verox.authentication.application.MerchantPrincipal;
import com.rightware.verox.common.web.ApiException;
import com.rightware.verox.payment.domain.Payment;
import com.rightware.verox.payment.domain.PaymentStatus;
import com.rightware.verox.payment.repository.PaymentRepository;
import com.rightware.verox.pilot.domain.PilotManualPaymentAcceptance;
import com.rightware.verox.pilot.repository.PilotManualPaymentAcceptanceRepository;
import com.rightware.verox.pilot.security.PilotManualAcceptanceAccessPolicy;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class PilotManualPaymentAcceptanceService {

    private final PaymentRepository paymentRepository;
    private final PilotManualPaymentAcceptanceRepository acceptanceRepository;
    private final PilotManualAcceptanceAccessPolicy accessPolicy;
    private final ApplicationEventPublisher eventPublisher;

    public PilotManualPaymentAcceptanceService(
        PaymentRepository paymentRepository,
        PilotManualPaymentAcceptanceRepository acceptanceRepository,
        PilotManualAcceptanceAccessPolicy accessPolicy,
        ApplicationEventPublisher eventPublisher
    ) {
        this.paymentRepository = paymentRepository;
        this.acceptanceRepository = acceptanceRepository;
        this.accessPolicy = accessPolicy;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public PilotManualPaymentAcceptanceView accept(
        MerchantPrincipal principal,
        String paymentPublicId,
        String reason
    ) {
        requirePilotAccess(principal);

        Payment payment = findScopedPayment(principal, paymentPublicId);

        PilotManualPaymentAcceptance existing = acceptanceRepository
            .findByPaymentIdAndMerchantId(payment.getId(), principal.merchantId())
            .orElse(null);

        if (existing != null) {
            return toView(payment, existing);
        }

        if (payment.getStatus() != PaymentStatus.PENDING
            && payment.getStatus() != PaymentStatus.REVIEW_REQUIRED) {
            throw new ApiException(
                HttpStatus.CONFLICT,
                "PILOT_MANUAL_ACCEPTANCE_NOT_ALLOWED",
                "Payment is not eligible for manual acceptance"
            );
        }

        PilotManualPaymentAcceptance acceptance = new PilotManualPaymentAcceptance(
            payment.getId(),
            principal.merchantId(),
            principal.apiKeyId(),
            reason
        );

        acceptance = acceptanceRepository.save(acceptance);

        eventPublisher.publishEvent(new PaymentManuallyAcceptedEvent(
            principal.merchantId(),
            payment.getPublicId(),
            payment.getCheckoutSession().getPublicId(),
            payment.getCheckoutSession().getExternalReference(),
            BigDecimal.valueOf(payment.getAmountMinor(), 2).toPlainString(),
            payment.getCurrency(),
            payment.getStatus().name(),
            acceptance.getAcceptedAt()
        ));

        return toView(payment, acceptance);
    }

    @Transactional(readOnly = true)
    public PilotManualPaymentAcceptanceView get(
        MerchantPrincipal principal,
        String paymentPublicId
    ) {
        requirePilotAccess(principal);
        Payment payment = findScopedPayment(principal, paymentPublicId);

        PilotManualPaymentAcceptance acceptance = acceptanceRepository
            .findByPaymentIdAndMerchantId(payment.getId(), principal.merchantId())
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "PILOT_MANUAL_ACCEPTANCE_NOT_FOUND",
                "Manual acceptance was not found"
            ));

        return toView(payment, acceptance);
    }

    private void requirePilotAccess(MerchantPrincipal principal) {
        if (principal == null || !accessPolicy.isAllowed(principal.merchantId())) {
            throw new ApiException(
                HttpStatus.FORBIDDEN,
                "PILOT_MANUAL_ACCEPTANCE_FORBIDDEN",
                "Manual acceptance is not enabled for this merchant"
            );
        }
    }

    private Payment findScopedPayment(MerchantPrincipal principal, String paymentPublicId) {
        Payment payment = paymentRepository
            .findByPublicIdAndMerchantId(paymentPublicId, principal.merchantId())
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "PAYMENT_NOT_FOUND",
                "Payment was not found"
            ));

        if (payment.getEnvironment() != principal.environment()) {
            throw new ApiException(
                HttpStatus.NOT_FOUND,
                "PAYMENT_NOT_FOUND",
                "Payment was not found"
            );
        }

        return payment;
    }

    private PilotManualPaymentAcceptanceView toView(
        Payment payment,
        PilotManualPaymentAcceptance acceptance
    ) {
        return new PilotManualPaymentAcceptanceView(
            payment.getPublicId(),
            "MANUALLY_ACCEPTED",
            acceptance.getReason(),
            acceptance.getAcceptedAt()
        );
    }
}
