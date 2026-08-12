package com.rightware.verox.checkout.application;

import com.rightware.verox.checkout.domain.CheckoutSession;
import com.rightware.verox.checkout.repository.CheckoutSessionRepository;
import com.rightware.verox.common.id.ResourceIdGenerator;
import com.rightware.verox.common.money.MoneyConverter;
import com.rightware.verox.common.money.MoneyConverter.MoneyValue;
import com.rightware.verox.common.web.ApiException;
import com.rightware.verox.merchant.domain.Merchant;
import com.rightware.verox.merchant.repository.MerchantRepository;
import com.rightware.verox.payment.domain.Payment;
import com.rightware.verox.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

@Service
public class CheckoutSessionService {

    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 255;
    private static final int MAX_EXTERNAL_REFERENCE_LENGTH = 160;
    private static final int MAX_DESCRIPTION_LENGTH = 255;

    private final CheckoutSessionRepository checkoutSessionRepository;
    private final PaymentRepository paymentRepository;
    private final MerchantRepository merchantRepository;
    private final MoneyConverter moneyConverter;
    private final ResourceIdGenerator resourceIdGenerator;
    private final RedirectUrlValidator redirectUrlValidator;
    private final CheckoutRequestFingerprint fingerprint;
    private final CheckoutSubmissionCapabilityService checkoutSubmissionCapabilityService;
    private final String checkoutBaseUrl;
    private final long sessionTtlMinutes;

    public CheckoutSessionService(
        CheckoutSessionRepository checkoutSessionRepository,
        PaymentRepository paymentRepository,
        MerchantRepository merchantRepository,
        MoneyConverter moneyConverter,
        ResourceIdGenerator resourceIdGenerator,
        RedirectUrlValidator redirectUrlValidator,
        CheckoutRequestFingerprint fingerprint,
        CheckoutSubmissionCapabilityService checkoutSubmissionCapabilityService,
        @Value("${verox.checkout.base-url:http://localhost:3000}") String checkoutBaseUrl,
        @Value("${verox.checkout.session-ttl-minutes:15}") long sessionTtlMinutes
    ) {
        this.checkoutSessionRepository = checkoutSessionRepository;
        this.paymentRepository = paymentRepository;
        this.merchantRepository = merchantRepository;
        this.moneyConverter = moneyConverter;
        this.resourceIdGenerator = resourceIdGenerator;
        this.redirectUrlValidator = redirectUrlValidator;
        this.fingerprint = fingerprint;
        this.checkoutSubmissionCapabilityService = checkoutSubmissionCapabilityService;
        this.checkoutBaseUrl = stripTrailingSlash(checkoutBaseUrl);
        this.sessionTtlMinutes = sessionTtlMinutes;
    }

    @Transactional
    public CheckoutSessionView create(CreateCheckoutSessionCommand command) {
        Merchant merchant = merchantRepository.findById(command.merchantId())
            .filter(Merchant::isActive)
            .orElseThrow(() -> new ApiException(
                HttpStatus.UNAUTHORIZED,
                "MERCHANT_UNAVAILABLE",
                "Merchant account is not available."
            ));

        String idempotencyKey = normalizeRequired(
            command.idempotencyKey(),
            MAX_IDEMPOTENCY_KEY_LENGTH,
            "INVALID_IDEMPOTENCY_KEY",
            "Idempotency-Key"
        );
        String externalReference = normalizeRequired(
            command.externalReference(),
            MAX_EXTERNAL_REFERENCE_LENGTH,
            "INVALID_EXTERNAL_REFERENCE",
            "external_reference"
        );
        String description = normalizeOptional(command.description(), MAX_DESCRIPTION_LENGTH, "description");
        MoneyValue money = moneyConverter.normalize(command.amount(), command.currency());
        String successUrl = redirectUrlValidator.validate(command.successUrl(), "success_url");
        String cancelUrl = redirectUrlValidator.validate(command.cancelUrl(), "cancel_url");

        String requestFingerprint = fingerprint.create(
            money.minor(),
            money.currency(),
            externalReference,
            description,
            successUrl,
            cancelUrl
        );

        var existing = checkoutSessionRepository.findByMerchantIdAndIdempotencyKey(merchant.getId(), idempotencyKey);
        if (existing.isPresent()) {
            CheckoutSession session = existing.orElseThrow();
            if (!Objects.equals(session.getRequestFingerprint(), requestFingerprint)) {
                throw new ApiException(
                    HttpStatus.CONFLICT,
                    "IDEMPOTENCY_CONFLICT",
                    "The Idempotency-Key was already used with different checkout parameters."
                );
            }

            Payment payment = paymentRepository.findByCheckoutSessionId(session.getId())
                .orElseThrow(() -> new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "PAYMENT_STATE_ERROR",
                    "Checkout Session exists without its Payment."
                ));
            return toView(session, payment);
        }

        Instant expiresAt = Instant.now().plus(sessionTtlMinutes, ChronoUnit.MINUTES);
        CheckoutSession session = new CheckoutSession(
            resourceIdGenerator.generate("cs"),
            merchant,
            command.environment(),
            externalReference,
            description,
            money.minor(),
            money.currency(),
            successUrl,
            cancelUrl,
            idempotencyKey,
            requestFingerprint,
            expiresAt
        );
        checkoutSessionRepository.save(session);

        Payment payment = new Payment(
            resourceIdGenerator.generate("pay"),
            merchant,
            session,
            command.environment(),
            money.minor(),
            money.currency()
        );
        paymentRepository.save(payment);

        return toView(session, payment);
    }

    @Transactional(readOnly = true)
    public CheckoutSessionView getForMerchant(UUID merchantId, String publicId) {
        CheckoutSession session = checkoutSessionRepository.findByPublicIdAndMerchantId(publicId, merchantId)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "CHECKOUT_SESSION_NOT_FOUND",
                "Checkout Session was not found."
            ));

        Payment payment = paymentRepository.findByCheckoutSessionId(session.getId())
            .orElseThrow(() -> new ApiException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "PAYMENT_STATE_ERROR",
                "Checkout Session exists without its Payment."
            ));

        return toView(session, payment);
    }

    private CheckoutSessionView toView(CheckoutSession session, Payment payment) {
        String capability = checkoutSubmissionCapabilityService.issue(session);
        return new CheckoutSessionView(
            session.getPublicId(),
            payment.getPublicId(),
            session.getExternalReference(),
            session.getStatus().name(),
            payment.getStatus().name(),
            moneyConverter.toMajorString(session.getAmountMinor()),
            session.getCurrency(),
            session.getDescription(),
            checkoutBaseUrl + "/c/" + session.getPublicId() + "#vx_capability=" + capability,
            session.getExpiresAt()
        );
    }

    private String normalizeRequired(String value, int maxLength, String code, String field) {
        if (value == null || value.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, code, field + " is required.");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new ApiException(HttpStatus.BAD_REQUEST, code, field + " is too long.");
        }
        return normalized;
    }

    private String normalizeOptional(String value, int maxLength, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", field + " is too long.");
        }
        return normalized;
    }

    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:3000";
        }
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
