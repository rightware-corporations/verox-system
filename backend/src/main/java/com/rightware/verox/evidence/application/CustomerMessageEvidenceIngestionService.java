package com.rightware.verox.evidence.application;

import com.rightware.verox.checkout.domain.CheckoutSession;
import com.rightware.verox.checkout.domain.CheckoutSessionStatus;
import com.rightware.verox.checkout.repository.CheckoutSessionRepository;
import com.rightware.verox.common.web.ApiException;
import com.rightware.verox.evidence.domain.Evidence;
import com.rightware.verox.evidence.domain.EvidenceIngestSource;
import com.rightware.verox.evidence.domain.EvidenceKind;
import com.rightware.verox.payment.domain.Payment;
import com.rightware.verox.payment.repository.PaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class CustomerMessageEvidenceIngestionService {

    private static final String MVP_PROVIDER = "MPESA";

    private final CheckoutSessionRepository checkoutSessionRepository;
    private final PaymentRepository paymentRepository;
    private final EvidenceService evidenceService;

    public CustomerMessageEvidenceIngestionService(
        CheckoutSessionRepository checkoutSessionRepository,
        PaymentRepository paymentRepository,
        EvidenceService evidenceService
    ) {
        this.checkoutSessionRepository = checkoutSessionRepository;
        this.paymentRepository = paymentRepository;
        this.evidenceService = evidenceService;
    }

    @Transactional
    public CustomerMessageEvidenceView ingest(String checkoutSessionId, String content) {
        CheckoutSession session = checkoutSessionRepository.findByPublicId(checkoutSessionId)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "CHECKOUT_SESSION_NOT_FOUND",
                "Checkout Session was not found."
            ));

        Instant receivedAt = Instant.now();
        if (!session.getExpiresAt().isAfter(receivedAt)) {
            throw new ApiException(
                HttpStatus.GONE,
                "CHECKOUT_SESSION_EXPIRED",
                "Checkout Session has expired."
            );
        }
        if (session.getStatus() != CheckoutSessionStatus.OPEN) {
            throw new ApiException(
                HttpStatus.CONFLICT,
                "CHECKOUT_SESSION_NOT_OPEN",
                "Checkout Session is not open for evidence submission."
            );
        }

        Payment payment = paymentRepository.findByCheckoutSessionId(session.getId())
            .orElseThrow(() -> new ApiException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "PAYMENT_STATE_ERROR",
                "Checkout Session exists without its Payment."
            ));

        Evidence evidence = evidenceService.registerCustomerRaw(
            payment,
            EvidenceKind.SMS,
            EvidenceIngestSource.HOSTED_CHECKOUT,
            MVP_PROVIDER,
            content,
            null,
            receivedAt
        );

        return new CustomerMessageEvidenceView(
            evidence.getPublicId(),
            session.getPublicId(),
            payment.getPublicId(),
            evidence.getOrigin().name(),
            evidence.getKind().name(),
            evidence.getIngestSource().name(),
            evidence.getProvider(),
            evidence.getReceivedAt()
        );
    }
}
