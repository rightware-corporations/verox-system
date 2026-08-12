package com.rightware.verox.verification.application;

import com.rightware.verox.authentication.domain.ApiKeyEnvironment;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
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
    private final Duration providerEvidenceGrace;

    public VerificationOrchestrator(
        PaymentRepository paymentRepository,
        EvidenceRepository evidenceRepository,
        EvidenceService evidenceService,
        MpesaMessageParser messageParser,
        MpesaEvidenceMatcher evidenceMatcher,
        ApplicationEventPublisher eventPublisher,
        @Value("${verox.verification.provider-evidence-grace-seconds:300}") long providerEvidenceGraceSeconds
    ) {
        this.paymentRepository = paymentRepository;
        this.evidenceRepository = evidenceRepository;
        this.evidenceService = evidenceService;
        this.messageParser = messageParser;
        this.evidenceMatcher = evidenceMatcher;
        this.eventPublisher = eventPublisher;
        this.providerEvidenceGrace = Duration.ofSeconds(Math.max(0, providerEvidenceGraceSeconds));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public VerificationRunResult verifyPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("Payment was not found"));
        return verify(payment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<VerificationRunResult> verifyMerchant(UUID merchantId, ApiKeyEnvironment environment) {
        List<VerificationRunResult> results = new ArrayList<>();
        List<Payment> payments = paymentRepository.findAllByMerchantIdAndEnvironmentAndStatusInOrderByCreatedAtAsc(
            merchantId,
            environment,
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
            .filter(evidence -> isCustomerMpesaMessageForPayment(evidence, payment))
            .toList();

        if (customerEvidence.isEmpty()) {
            return result(payment, VerificationRunStatus.WAITING_CUSTOMER, "CUSTOMER_EVIDENCE_NOT_AVAILABLE", null, null);
        }

        Map<String, CustomerCandidate> matchReadyCustomers = new LinkedHashMap<>();
        for (Evidence evidence : customerEvidence) {
            ParsedMpesaMessage parsed = messageParser.parse(EvidenceOrigin.CUSTOMER, evidence.getRawContent());
            if (!isExpectedCustomerCandidate(payment, parsed)) {
                continue;
            }
            String key = parsed.transactionReference().toUpperCase(Locale.ROOT)
                + "|" + parsed.amountMinor()
                + "|" + parsed.currency().toUpperCase(Locale.ROOT);
            matchReadyCustomers.putIfAbsent(key, new CustomerCandidate(evidence, parsed));
        }

        if (matchReadyCustomers.isEmpty()) {
            return result(
                payment,
                VerificationRunStatus.WAITING_CUSTOMER,
                "CUSTOMER_EVIDENCE_NOT_MATCH_READY",
                null,
                null
            );
        }

        Map<String, CustomerCandidate> unusedCustomers = new LinkedHashMap<>();
        for (Map.Entry<String, CustomerCandidate> entry : matchReadyCustomers.entrySet()) {
            String transactionReference = entry.getValue().parsed().transactionReference().toUpperCase(Locale.ROOT);
            boolean alreadyUsed = paymentRepository
                .existsByMerchantIdAndEnvironmentAndProviderIgnoreCaseAndProviderTransactionReferenceIgnoreCaseAndIdNot(
                    payment.getMerchant().getId(),
                    payment.getEnvironment(),
                    MVP_PROVIDER,
                    transactionReference,
                    payment.getId()
                );
            if (!alreadyUsed) {
                unusedCustomers.put(entry.getKey(), entry.getValue());
            }
        }

        if (unusedCustomers.isEmpty()) {
            return result(
                payment,
                VerificationRunStatus.WAITING_CUSTOMER,
                "CUSTOMER_TRANSACTION_REFERENCE_ALREADY_USED",
                null,
                null
            );
        }

        payment.beginVerification();

        Instant eligibleFrom = payment.getCheckoutSession().getCreatedAt();
        Instant eligibleTo = payment.getCheckoutSession().getExpiresAt().plus(providerEvidenceGrace);

        List<ProviderCandidate> providerCandidates = evidenceRepository
            .findAllByMerchantIdAndEnvironmentAndOriginAndProviderIgnoreCaseAndPaymentIsNullAndCreatedAtBetweenOrderByCreatedAtAsc(
                payment.getMerchant().getId(),
                payment.getEnvironment(),
                EvidenceOrigin.PROVIDER,
                MVP_PROVIDER,
                eligibleFrom,
                eligibleTo
            )
            .stream()
            .filter(this::isProviderMpesaMessage)
            .map(evidence -> new ProviderCandidate(
                evidence,
                messageParser.parse(EvidenceOrigin.PROVIDER, evidence.getRawContent())
            ))
            .filter(candidate -> candidate.parsed().isMatchReady())
            .toList();

        List<MatchedPair> matchedPairs = new ArrayList<>();
        ProviderConflict providerConflict = null;

        for (CustomerCandidate customer : unusedCustomers.values()) {
            String transactionReference = customer.parsed().transactionReference().toUpperCase(Locale.ROOT);
            List<ProviderCandidate> sameReferenceProviders = providerCandidates.stream()
                .filter(candidate -> candidate.parsed().transactionReference().equalsIgnoreCase(transactionReference))
                .toList();

            if (sameReferenceProviders.isEmpty()) {
                continue;
            }

            if (sameReferenceProviders.size() > 1) {
                if (providerConflict == null) {
                    providerConflict = new ProviderConflict(
                        "PROVIDER_EVIDENCE_AMBIGUOUS",
                        null,
                        transactionReference
                    );
                }
                continue;
            }

            ProviderCandidate provider = sameReferenceProviders.getFirst();
            VerificationMatchResult match = evidenceMatcher.match(
                customer.parsed(),
                provider.parsed(),
                payment.getAmountMinor(),
                payment.getCurrency()
            );

            if (match.isMatch()) {
                matchedPairs.add(new MatchedPair(customer, provider, match));
                continue;
            }

            if (providerConflict == null) {
                providerConflict = new ProviderConflict(
                    match.reason(),
                    provider.evidence().getPublicId(),
                    transactionReference
                );
            }
        }

        if (matchedPairs.isEmpty()) {
            if (providerConflict != null) {
                payment.requireReview();
                paymentRepository.save(payment);
                return result(
                    payment,
                    VerificationRunStatus.REVIEW_REQUIRED,
                    providerConflict.reason(),
                    providerConflict.providerEvidenceId(),
                    providerConflict.transactionReference()
                );
            }

            return result(
                payment,
                VerificationRunStatus.WAITING_PROVIDER,
                "MATCHING_PROVIDER_EVIDENCE_NOT_AVAILABLE",
                null,
                null
            );
        }

        if (matchedPairs.size() > 1) {
            payment.requireReview();
            paymentRepository.save(payment);
            return result(
                payment,
                VerificationRunStatus.REVIEW_REQUIRED,
                "MULTIPLE_MATCHED_EVIDENCE_PAIRS",
                null,
                null
            );
        }

        MatchedPair selected = matchedPairs.getFirst();
        String transactionReference = selected.customer().parsed().transactionReference().toUpperCase(Locale.ROOT);

        evidenceService.linkProviderEvidence(selected.provider().evidence(), payment);
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
            selected.match().reason(),
            selected.provider().evidence().getPublicId(),
            transactionReference
        );
    }

    private boolean isExpectedCustomerCandidate(Payment payment, ParsedMpesaMessage parsed) {
        return parsed.isMatchReady()
            && parsed.amountMinor() == payment.getAmountMinor()
            && parsed.currency().equalsIgnoreCase(payment.getCurrency());
    }

    private boolean isCustomerMpesaMessageForPayment(Evidence evidence, Payment payment) {
        return evidence.getOrigin() == EvidenceOrigin.CUSTOMER
            && evidence.getEnvironment() == payment.getEnvironment()
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

    private record MatchedPair(
        CustomerCandidate customer,
        ProviderCandidate provider,
        VerificationMatchResult match
    ) {
    }

    private record ProviderConflict(
        String reason,
        String providerEvidenceId,
        String transactionReference
    ) {
    }
}
