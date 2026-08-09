package com.rightware.verox.evidence.application;

import com.rightware.verox.common.id.ResourceIdGenerator;
import com.rightware.verox.evidence.domain.Evidence;
import com.rightware.verox.evidence.domain.EvidenceIngestSource;
import com.rightware.verox.evidence.domain.EvidenceKind;
import com.rightware.verox.evidence.domain.EvidenceOrigin;
import com.rightware.verox.evidence.repository.EvidenceRepository;
import com.rightware.verox.payment.domain.Payment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

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
    public Evidence registerRaw(
        Payment payment,
        EvidenceOrigin origin,
        EvidenceKind kind,
        EvidenceIngestSource ingestSource,
        String provider,
        String rawContent,
        Instant occurredAt,
        Instant receivedAt
    ) {
        Objects.requireNonNull(payment, "payment");
        String contentSha256 = contentHasher.sha256(rawContent);

        return existing(payment, origin, kind, contentSha256)
            .orElseGet(() -> evidenceRepository.save(new Evidence(
                resourceIdGenerator.generate("ev"),
                payment.getMerchant(),
                payment,
                origin,
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
    public Evidence registerStored(
        Payment payment,
        EvidenceOrigin origin,
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

        return existing(payment, origin, kind, contentSha256)
            .orElseGet(() -> evidenceRepository.save(new Evidence(
                resourceIdGenerator.generate("ev"),
                payment.getMerchant(),
                payment,
                origin,
                kind,
                ingestSource,
                provider,
                contentSha256,
                contentType,
                originalFilename,
                storageKey,
                null,
                occurredAt,
                receivedAt
            )));
    }

    private java.util.Optional<Evidence> existing(
        Payment payment,
        EvidenceOrigin origin,
        EvidenceKind kind,
        String contentSha256
    ) {
        return evidenceRepository.findByPaymentIdAndOriginAndKindAndContentSha256(
            payment.getId(),
            origin,
            kind,
            contentSha256.toLowerCase()
        );
    }
}
