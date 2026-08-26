package com.rightware.verox.evidence.application;

import com.rightware.verox.checkout.application.CheckoutSubmissionCapabilityService;
import com.rightware.verox.checkout.domain.CheckoutSession;
import com.rightware.verox.checkout.domain.CheckoutSessionStatus;
import com.rightware.verox.checkout.repository.CheckoutSessionRepository;
import com.rightware.verox.common.ratelimit.RateLimitGuard;
import com.rightware.verox.common.web.ApiException;
import com.rightware.verox.evidence.domain.Evidence;
import com.rightware.verox.evidence.domain.EvidenceIngestSource;
import com.rightware.verox.evidence.domain.EvidenceKind;
import com.rightware.verox.payment.domain.Payment;
import com.rightware.verox.payment.repository.PaymentRepository;
import com.rightware.verox.paymentchannel.application.PaymentChannelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
public class CustomerMessageEvidenceIngestionService {

    private final CheckoutSessionRepository checkoutSessionRepository;
    private final PaymentRepository paymentRepository;
    private final EvidenceService evidenceService;
    private final ApplicationEventPublisher eventPublisher;
    private final CheckoutSubmissionCapabilityService checkoutSubmissionCapabilityService;
    private final RateLimitGuard rateLimitGuard;
    private final PaymentChannelService paymentChannelService;

    @Autowired
    public CustomerMessageEvidenceIngestionService(
        CheckoutSessionRepository checkoutSessionRepository,
        PaymentRepository paymentRepository,
        EvidenceService evidenceService,
        ApplicationEventPublisher eventPublisher,
        CheckoutSubmissionCapabilityService checkoutSubmissionCapabilityService,
        RateLimitGuard rateLimitGuard,
        PaymentChannelService paymentChannelService
    ) {
        this.checkoutSessionRepository = checkoutSessionRepository;
        this.paymentRepository = paymentRepository;
        this.evidenceService = evidenceService;
        this.eventPublisher = eventPublisher;
        this.checkoutSubmissionCapabilityService = checkoutSubmissionCapabilityService;
        this.rateLimitGuard = rateLimitGuard;
        this.paymentChannelService = paymentChannelService;
    }

    public CustomerMessageEvidenceIngestionService(
        CheckoutSessionRepository checkoutSessionRepository,
        PaymentRepository paymentRepository,
        EvidenceService evidenceService,
        ApplicationEventPublisher eventPublisher,
        CheckoutSubmissionCapabilityService checkoutSubmissionCapabilityService,
        RateLimitGuard rateLimitGuard
    ) {
        this(
            checkoutSessionRepository,
            paymentRepository,
            evidenceService,
            eventPublisher,
            checkoutSubmissionCapabilityService,
            rateLimitGuard,
            null
        );
    }

    @Deprecated
    public CustomerMessageEvidenceView ingest(
        String checkoutSessionId,
        String checkoutCapability,
        String content
    ) {
        return ingest(checkoutSessionId, checkoutCapability, "MPESA", content);
    }

    @Transactional
    public CustomerMessageEvidenceView ingest(
        String checkoutSessionId,
        String checkoutCapability,
        String provider,
        String content
    ) {
        CheckoutSession session = checkoutSessionRepository
            .findByPublicId(checkoutSessionId)
            .orElseThrow(this::checkoutNotFound);

        if (!checkoutSubmissionCapabilityService.matches(session, checkoutCapability)) {
            throw checkoutNotFound();
        }

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

        rateLimitGuard.checkCheckoutSubmission(session.getId());

        Payment payment = paymentRepository
            .findByCheckoutSessionId(session.getId())
            .orElseThrow(() -> new ApiException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "PAYMENT_STATE_ERROR",
                "Checkout Session exists without its Payment."
            ));

        String normalizedProvider = normalizeProvider(provider);
        validateActiveProvider(session, normalizedProvider);

        Evidence evidence = evidenceService.registerCustomerRaw(
            payment,
            EvidenceKind.SMS,
            EvidenceIngestSource.HOSTED_CHECKOUT,
            normalizedProvider,
            content,
            null,
            receivedAt
        );

        eventPublisher.publishEvent(
            new EvidenceIngestedEvent(
                payment.getMerchant().getId(),
                payment.getId(),
                evidence.getEnvironment(),
                evidence.getOrigin(),
                evidence.getPublicId()
            )
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

    private String normalizeProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            throw invalidProvider();
        }
        return provider.trim().toUpperCase(Locale.ROOT);
    }

    private void validateActiveProvider(CheckoutSession session, String provider) {
        if (paymentChannelService == null) {
            return;
        }
        boolean allowed = paymentChannelService
            .listActiveForCheckout(session.getMerchant().getId(), session.getEnvironment())
            .stream()
            .anyMatch(channel -> channel.provider().equalsIgnoreCase(provider));
        if (!allowed) {
            throw invalidProvider();
        }
    }

    private ApiException invalidProvider() {
        return new ApiException(
            HttpStatus.BAD_REQUEST,
            "INVALID_PAYMENT_PROVIDER",
            "Selected payment provider is not available for this checkout."
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
