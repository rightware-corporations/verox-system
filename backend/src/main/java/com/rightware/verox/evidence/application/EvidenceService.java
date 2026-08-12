package com.rightware.verox.evidence.application;

import com.rightware.verox.authentication.domain.ApiKeyEnvironment;
import com.rightware.verox.common.id.ResourceIdGenerator;
import com.rightware.verox.evidence.domain.Evidence;
import com.rightware.verox.evidence.domain.EvidenceIngestSource;
import com.rightware.verox.evidence.domain.EvidenceKind;
import com.rightware.verox.evidence.domain.EvidenceOrigin;
import com.rightware.verox.evidence.repository.EvidenceRepository;
import com.rightware.verox.merchant.domain.Merchant;
import com.rightware.verox.payment.domain.Payment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class EvidenceService {

    private final EvidenceRepository evidenceRepository;
    private final EvidenceContentHasher contentHasher;
    private final ResourceIdGenerator resourceIdGenerator;

    public EvidenceService(
        EvidenceRepository evidenceRepository,
        EvidenceContentHasher contentHasher,
        ResourceIdGenerator resourceIdGenerator
    ) {
        this.evidenceRepository = evidenceRepository;
        this.contentHasher = contentHasher;
        this.resourceIdGenerator = resourceIdGenerator;
    }

    @Transactional
    public Evidence registerProviderRaw(
        Merchant merchant,
        ApiKeyEnvironment environment,
        EvidenceKind kind,
        EvidenceIngestSource ingestSource,
        String provider,
        String rawContent,
        Instant occurredAt,
        Instant receivedAt
    ) {
        Objects.requireNonNull(merchant, "merchant");
        Objects.requireNonNull(environment, "environment");
        String contentSha256 = contentHasher.sha256(rawContent);

        return evidenceRepository.findByMerchantIdAndEnvironmentAndOriginAndKindAndContentSha256(
            merchant.getId(),
            environment,
            EvidenceOrigin.PROVIDER,
            kind,
            contentSha256
        ).orElseGet(() -> evidenceRepository.save(new Evidence(
            resourceIdGenerator.generate("ev"),
            merchant,
            null,
            environment,
            EvidenceOrigin.PROVIDER,
            kind,
            ingestSource,
            provider,
            contentSha256,
            "text/plain",
            null,
            null,
            rawContent,
            occurredAt,
            receivedAt
        )));
    }

    @Transactional
    public Evidence registerCustomerRaw(
        Payment payment,
        EvidenceKind kind,
        EvidenceIngestSource ingestSource,
        String provider,
        String rawContent,
        Instant occurredAt,
        Instant receivedAt
    ) {
        Objects.requireNonNull(payment, "payment");
        String contentSha256 = contentHasher.sha256(rawContent);

        return evidenceRepository.findByPaymentIdAndOriginAndKindAndContentSha256(
            payment.getId(),
            EvidenceOrigin.CUSTOMER,
            kind,
            contentSha256
        ).orElseGet(() -> evidenceRepository.save(new Evidence(
            resourceIdGenerator.generate("ev"),
            payment.getMerchant(),
            payment,
            payment.getEnvironment(),
            EvidenceOrigin.CUSTOMER,
            kind,
            ingestSource,
            provider,
            contentSha256,
            "text/plain",
            null,
            null,
            rawContent,
            occurredAt,
            receivedAt
        )));
    }

    @Transactional
    public Evidence registerCustomerStored(
        Payment payment,
        EvidenceKind kind,
        EvidenceIngestSource ingestSource,
        String provider,
        String contentSha256,
        String contentType,
        String originalFilename,
        String storageKey,
        Instant occurredAt,
        Instant receivedAt
    ) {
        Objects.requireNonNull(payment, "payment");
        String normalizedHash = Objects.requireNonNull(contentSha256, "contentSha256").toLowerCase();

        return evidenceRepository.findByPaymentIdAndOriginAndKindAndContentSha256(
            payment.getId(),
            EvidenceOrigin.CUSTOMER,
            kind,
            normalizedHash
        ).orElseGet(() -> evidenceRepository.save(new Evidence(
            resourceIdGenerator.generate("ev"),
            payment.getMerchant(),
            payment,
            payment.getEnvironment(),
            EvidenceOrigin.CUSTOMER,
            kind,
            ingestSource,
            provider,
            normalizedHash,
            contentType,
            originalFilename,
            storageKey,
            null,
            occurredAt,
            receivedAt
        )));
    }

    @Transactional
    public Evidence linkProviderEvidence(Evidence evidence, Payment payment) {
        Objects.requireNonNull(evidence, "evidence");
        Objects.requireNonNull(payment, "payment");
        if (evidence.getOrigin() != EvidenceOrigin.PROVIDER) {
            throw new IllegalArgumentException("Only provider evidence can be linked by the Verification Engine");
        }
        evidence.linkToPayment(payment);
        return evidenceRepository.save(evidence);
    }

    @Transactional(readOnly = true)
    public Optional<Evidence> findForMerchant(UUID merchantId, String publicId) {
        return evidenceRepository.findByPublicIdAndMerchantId(publicId, merchantId);
    }
}
