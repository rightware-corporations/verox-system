package com.rightware.verox.pilot.application;

import com.rightware.verox.authentication.application.MerchantOperatorPrincipal;
import com.rightware.verox.authentication.application.MerchantPrincipal;
import com.rightware.verox.authentication.domain.ApiKeyEnvironment;
import com.rightware.verox.common.web.ApiException;
import com.rightware.verox.payment.domain.Payment;
import com.rightware.verox.payment.domain.PaymentStatus;
import com.rightware.verox.payment.repository.PaymentRepository;
import com.rightware.verox.pilot.domain.PilotManualPaymentRejection;
import com.rightware.verox.pilot.repository.PilotManualPaymentRejectionRepository;
import com.rightware.verox.pilot.security.PilotManualAcceptanceAccessPolicy;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PilotManualPaymentRejectionService {
    private final PaymentRepository paymentRepository;
    private final PilotManualPaymentRejectionRepository rejectionRepository;
    private final PilotManualAcceptanceAccessPolicy accessPolicy;
    private final ApplicationEventPublisher eventPublisher;

    public PilotManualPaymentRejectionService(PaymentRepository paymentRepository, PilotManualPaymentRejectionRepository rejectionRepository, PilotManualAcceptanceAccessPolicy accessPolicy, ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository; this.rejectionRepository = rejectionRepository; this.accessPolicy = accessPolicy; this.eventPublisher = eventPublisher;
    }

    @Transactional
    public PilotManualPaymentRejectionView reject(MerchantOperatorPrincipal principal, String paymentPublicId, String reason) {
        requirePilotAccess(principal.merchantId());
        validateReason(reason);
        Payment payment = findScopedPayment(principal.merchantId(), principal.environment(), paymentPublicId);
        PilotManualPaymentRejection existing = rejectionRepository.findByPaymentIdAndMerchantId(payment.getId(), principal.merchantId()).orElse(null);
        if (existing != null) return toView(payment, existing);
        if (payment.getStatus() != PaymentStatus.PENDING && payment.getStatus() != PaymentStatus.REVIEW_REQUIRED) {
            throw new ApiException(HttpStatus.CONFLICT, "PILOT_MANUAL_REJECTION_NOT_ALLOWED", "Payment is not eligible for manual rejection");
        }
        PilotManualPaymentRejection rejection = rejectionRepository.save(new PilotManualPaymentRejection(payment.getId(), principal.merchantId(), null, principal.operatorId(), reason));
        eventPublisher.publishEvent(new PaymentManuallyRejectedEvent(principal.merchantId(), payment.getPublicId(), payment.getCheckoutSession().getPublicId(), payment.getCheckoutSession().getExternalReference(), BigDecimal.valueOf(payment.getAmountMinor(), 2).toPlainString(), payment.getCurrency(), payment.getStatus().name(), rejection.getRejectedAt()));
        return toView(payment, rejection);
    }

    @Transactional
    public PilotManualPaymentRejectionView reject(MerchantPrincipal principal, String paymentPublicId, String reason) {
        requirePilotAccess(principal.merchantId());
        validateReason(reason);
        Payment payment = findScopedPayment(principal.merchantId(), principal.environment(), paymentPublicId);
        PilotManualPaymentRejection existing = rejectionRepository.findByPaymentIdAndMerchantId(payment.getId(), principal.merchantId()).orElse(null);
        if (existing != null) return toView(payment, existing);
        if (payment.getStatus() != PaymentStatus.PENDING && payment.getStatus() != PaymentStatus.REVIEW_REQUIRED) throw new ApiException(HttpStatus.CONFLICT, "PILOT_MANUAL_REJECTION_NOT_ALLOWED", "Payment is not eligible for manual rejection");
        PilotManualPaymentRejection rejection = rejectionRepository.save(new PilotManualPaymentRejection(payment.getId(), principal.merchantId(), principal.apiKeyId(), null, reason));
        eventPublisher.publishEvent(new PaymentManuallyRejectedEvent(principal.merchantId(), payment.getPublicId(), payment.getCheckoutSession().getPublicId(), payment.getCheckoutSession().getExternalReference(), BigDecimal.valueOf(payment.getAmountMinor(), 2).toPlainString(), payment.getCurrency(), payment.getStatus().name(), rejection.getRejectedAt()));
        return toView(payment, rejection);
    }

    private void validateReason(String reason) {
        if (reason != null && reason.length() > 255) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REASON", "Reason must not exceed 255 characters");
    }
    private void requirePilotAccess(UUID merchantId) { if (merchantId == null || !accessPolicy.isAllowed(merchantId)) throw new ApiException(HttpStatus.FORBIDDEN, "PILOT_MANUAL_REJECTION_FORBIDDEN", "Manual rejection is not enabled for this merchant"); }
    private Payment findScopedPayment(UUID merchantId, ApiKeyEnvironment environment, String publicId) {
        Payment payment = paymentRepository.findByPublicIdAndMerchantId(publicId, merchantId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment was not found"));
        if (payment.getEnvironment() != environment) throw new ApiException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment was not found");
        return payment;
    }
    private PilotManualPaymentRejectionView toView(Payment payment, PilotManualPaymentRejection rejection) { return new PilotManualPaymentRejectionView(payment.getPublicId(), "MANUALLY_REJECTED", rejection.getReason(), rejection.getRejectedAt()); }
}
