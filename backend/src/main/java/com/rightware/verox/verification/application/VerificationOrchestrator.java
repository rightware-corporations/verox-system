package com.rightware.verox.verification.application;

import com.rightware.verox.evidence.application.EvidenceService;
import com.rightware.verox.evidence.domain.Evidence;
import com.rightware.verox.evidence.domain.EvidenceIngestSource;
import com.rightware.verox.evidence.domain.EvidenceKind;
import com.rightware.verox.evidence.domain.EvidenceOrigin;
import com.rightware.verox.evidence.repository.EvidenceRepository;
import com.rightware.verox.payment.application.PaymentConfirmedEvent;
import com.rightware.verox.payment.domain.Payment;
import com.rightware.verox.payment.domain.PaymentStatus;
import com.rightware.verox.payment.repository.PaymentRepository;
import com.rightware.verox.verification.matching.MpesaEvidenceMatcher;
import com.rightware.verox.verification.matching.VerificationMatchResult;
import com.rightware.verox.verification.mpesa.MpesaMessageParser;
import com.rightware.verox.verification.mpesa.ParsedMpesaMessage;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class VerificationOrchestrator {

    private static final String MVP_PROVIDER = "MPESA";
    private static final EnumSet<PaymentStatus> ACTIVE_STATUSES = EnumSet.of(
        PaymentStatus.PENDING,
        PaymentStatus.VERIFYING
    );

    private final PaymentRepository paymentRepository;
    private final EvidenceRepository evidenceRepository;
    private final EvidenceService evidenceService;
    private final MpesaMessageParser messageParser;
    private final MpesaEvidenceMatcher evidenceMatcher;
    private final ApplicationEventPublisher eventPublisher;

    public VerificationOrchestrator(
        PaymentRepository paymentRepository,
        EvidenceRepository evidenceRepository,
        EvidenceService evidenceService,
        MpesaMessageParser messageParser,
        MpesaEvidenceMatcher evidenceMatcher,
        ApplicationEventPublisher eventPublisher
    ) {
        this.paymentRepository = paymentRepository;
        this.evidenceRepository = evidenceRepository;
        this.evidenceService = evidenceService;
        this.messageParser = messageParser;
        this.evidenceMatcher = evidenceMatcher;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public VerificationRunResult verifyPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("Payment was not found"));
        return verify(payment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<VerificationRunResult> verifyMerchant(UUID merchantId) {
        List<VerificationRunResult> results = new ArrayList<>();
        List<Payment> payments = paymentRepository.findAllByMerchantIdAndStatusInOrderByCreatedAtAsc(
            merchantId,
            ACTIVE_STATUSES
        );
        for (Payment payment : payments) {
            results.add(verify(payment));
        }
        return results;
    }

    private VerificationRunResult verify(Payment payment) {
        if (!ACTIVE_STATUSES.contains(payment.getStatus())) {
            return result(payment, VerificationRunStatus.SKIPPED_TERMINAL, "PAYMENT_NOT_ACTIVE", null, null);
        }

        List<Evidence> customerEvidence = evidenceRepository.findAllByPaymentIdOrderByReceivedAtAsc(payment.getId())
            .stream()
            .filter(this::isCustomerMpesaMessage)
            .toList();

        if (customerEvidence.isEmpty()) {
            return result(payment, VerificationRunStatus.WAITING_CUSTOMER, "CUSTOMER_EVIDENCE_NOT_AVAILABLE", null, null);
        }

        Map<String, CustomerCandidate> distinctCustomers = new LinkedHashMap<>();
        for (Evidence evidence : customerEvidence) {
            ParsedMpesaMessage parsed = messageParser.parse(EvidenceOrigin.CUSTOMER, evidence.getRawContent());
            if (!parsed.isMatchReady()) {
                continue;
            }
            String key = parsed.transactionReference().toUpperCase(Locale.ROOT)
                + "|" + parsed.amountMinor()
                + "|" + parsed.currency().toUpperCase(Locale.ROOT);
            distinctCustomers.putIfAbsent(key, new CustomerCandidate(evidence, parsed));
        }

        if (distinctCustomers.isEmpty()) {
            payment.requireReview();
            paymentRepository.save(payment);
            return result(payment, VerificationRunStatus.REVIEW_REQUIRED, "CUSTOMER_MESSAGE_UNRECOGNIZED", null, null);
        }

        if (distinctCustomers.size() > 1) {
            payment.requireReview();
            paymentRepository.save(payment);
            return result(payment, VerificationRunStatus.REVIEW_REQUIRED, "CUSTOMER_EVIDENCE_AMBIGUOUS", null, null);
        }

        CustomerCandidate customer = distinctCustomers.values().iterator().next();
        if (customer.parsed().amountMinor() != payment.getAmountMinor()) {
            payment.requireReview();
            paymentRepository.save(payment);
            return result(payment, VerificationRunStatus.REVIEW_REQUIRED, "CUSTOMER_PAYMENT_AMOUNT_MISMATCH", null, customer.parsed().transactionReference());
        }
        if (!customer.parsed().currency().equalsIgnoreCase(payment.getCurrency())) {
            payment.requireReview();
            paymentRepository.save(payment);
            return result(payment, VerificationRunStatus.REVIEW_REQUIRED, "CUSTOMER_PAYMENT_CURRENCY_MISMATCH", null, customer.parsed().transactionReference());
        }

        String transactionReference = customer.parsed().transactionReference().toUpperCase(Locale.ROOT);
        if (paymentRepository.existsByMerchantIdAndProviderIgnoreCaseAndProviderTransactionReferenceIgnoreCaseAndIdNot(
            payment.getMerchant().getId(),
            MVP_PROVIDER,
            transactionReference,
            payment.getId()
        )) {
            payment.requireReview();
            paymentRepository.save(payment);
            return result(payment, VerificationRunStatus.REVIEW_REQUIRED, "TRANSACTION_REFERENCE_ALREADY_USED", null, transactionReference);
        }

        payment.beginVerification();

        List<ProviderCandidate> sameReferenceProviders = evidenceRepository
            .findAllByMerchantIdAndOriginAndPaymentIsNullOrderByReceivedAtAsc(
                payment.getMerchant().getId(),
                EvidenceOrigin.PROVIDER
            )
            .stream()
            .filter(this::isProviderMpesaMessage)
            .map(evidence -> new ProviderCandidate(
                evidence,
                messageParser.parse(EvidenceOrigin.PROVIDER, evidence.getRawContent())
            ))
            .filter(candidate -> candidate.parsed().isMatchReady())
            .filter(candidate -> candidate.parsed().transactionReference().equalsIgnoreCase(transactionReference))
            .toList();

        if (sameReferenceProviders.isEmpty()) {
            return result(payment, VerificationRunStatus.WAITING_PROVIDER, "MATCHING_PROVIDER_EVIDENCE_NOT_AVAILABLE", null, transactionReference);
        }

        if (sameReferenceProviders.size() > 1) {
            payment.requireReview();
            paymentRepository.save(payment);
            return result(payment, VerificationRunStatus.REVIEW_REQUIRED, "PROVIDER_EVIDENCE_AMBIGUOUS", null, transactionReference);
        }

        ProviderCandidate provider = sameReferenceProviders.getFirst();
        VerificationMatchResult match = evidenceMatcher.match(
            customer.parsed(),
            provider.parsed(),
            payment.getAmountMinor(),
            payment.getCurrency()
        );

        if (!match.isMatch()) {
            payment.requireReview();
            paymentRepository.save(payment);
            return result(
                payment,
                VerificationRunStatus.REVIEW_REQUIRED,
                match.reason(),
                provider.evidence().getPublicId(),
                transactionReference
            );
        }

        evidenceService.linkProviderEvidence(provider.evidence(), payment);
        Instant confirmedAt = Instant.now();
        payment.confirm(MVP_PROVIDER, transactionReference, confirmedAt);
        payment.getCheckoutSession().complete(confirmedAt);
        paymentRepository.save(payment);

        eventPublisher.publishEvent(new PaymentConfirmedEvent(
            payment.getMerchant().getId(),
            payment.getPublicId(),
            payment.getCheckoutSession().getPublicId(),
            payment.getCheckoutSession().getExternalReference(),
            BigDecimal.valueOf(payment.getAmountMinor(), 2).toPlainString(),
            payment.getCurrency(),
            payment.getProvider(),
            payment.getProviderTransactionReference(),
            payment.getConfirmedAt()
        ));

        return result(
            payment,
            VerificationRunStatus.CONFIRMED,
            match.reason(),
            provider.evidence().getPublicId(),
            transactionReference
        );
    }

    private boolean isCustomerMpesaMessage(Evidence evidence) {
        return evidence.getOrigin() == EvidenceOrigin.CUSTOMER
            && evidence.getKind() == EvidenceKind.SMS
            && evidence.getIngestSource() == EvidenceIngestSource.HOSTED_CHECKOUT
            && MVP_PROVIDER.equalsIgnoreCase(evidence.getProvider())
            && evidence.getRawContent() != null;
    }

    private boolean isProviderMpesaMessage(Evidence evidence) {
        return evidence.getOrigin() == EvidenceOrigin.PROVIDER
            && evidence.getKind() == EvidenceKind.SMS
            && evidence.getIngestSource() == EvidenceIngestSource.VEROX_BRIDGE
            && MVP_PROVIDER.equalsIgnoreCase(evidence.getProvider())
            && evidence.getRawContent() != null;
    }

    private VerificationRunResult result(
        Payment payment,
        VerificationRunStatus status,
        String reason,
        String providerEvidenceId,
        String transactionReference
    ) {
        return new VerificationRunResult(
            payment.getPublicId(),
            status,
            reason,
            providerEvidenceId,
            transactionReference
        );
    }

    private record CustomerCandidate(Evidence evidence, ParsedMpesaMessage parsed) {
    }

    private record ProviderCandidate(Evidence evidence, ParsedMpesaMessage parsed) {
    }
}
