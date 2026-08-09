package com.rightware.verox.evidence.application;

import com.rightware.verox.common.id.ResourceIdGenerator;
import com.rightware.verox.evidence.domain.Evidence;
import com.rightware.verox.evidence.domain.EvidenceIngestSource;
import com.rightware.verox.evidence.domain.EvidenceKind;
import com.rightware.verox.evidence.domain.EvidenceOrigin;
import com.rightware.verox.evidence.repository.EvidenceRepository;
import com.rightware.verox.merchant.domain.Merchant;
import com.rightware.verox.payment.domain.Payment;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EvidenceServiceTest {

    @Test
    void registersRawProviderEvidenceWithVeroxEvidenceId() {
        EvidenceRepository repository = mock(EvidenceRepository.class);
        ResourceIdGenerator ids = mock(ResourceIdGenerator.class);
        EvidenceContentHasher hasher = new EvidenceContentHasher();
        Payment payment = mock(Payment.class);
        Merchant merchant = new Merchant("Event Merchant");
        UUID paymentId = UUID.randomUUID();

        when(payment.getId()).thenReturn(paymentId);
        when(payment.getMerchant()).thenReturn(merchant);
        when(ids.generate("ev")).thenReturn("ev_test123");
        when(repository.findByPaymentIdAndOriginAndKindAndContentSha256(
            paymentId,
            EvidenceOrigin.PROVIDER,
            EvidenceKind.SMS,
            hasher.sha256("M-Pesa official SMS")
        )).thenReturn(Optional.empty());
        when(repository.save(any(Evidence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EvidenceService service = new EvidenceService(repository, hasher, ids);
        Evidence evidence = service.registerRaw(
            payment,
            EvidenceOrigin.PROVIDER,
            EvidenceKind.SMS,
            EvidenceIngestSource.VEROX_BRIDGE,
            "MPESA",
            "M-Pesa official SMS",
            Instant.parse("2026-08-09T12:00:00Z"),
            Instant.parse("2026-08-09T12:00:02Z")
        );

        assertThat(evidence.getPublicId()).isEqualTo("ev_test123");
        assertThat(evidence.getOrigin()).isEqualTo(EvidenceOrigin.PROVIDER);
        assertThat(evidence.getKind()).isEqualTo(EvidenceKind.SMS);
        assertThat(evidence.getIngestSource()).isEqualTo(EvidenceIngestSource.VEROX_BRIDGE);
        assertThat(evidence.getProvider()).isEqualTo("MPESA");
        assertThat(evidence.getRawContent()).isEqualTo("M-Pesa official SMS");
        assertThat(evidence.getContentSha256()).hasSize(64);
        verify(repository).save(any(Evidence.class));
    }

    @Test
    void returnsExistingEvidenceForDuplicateContent() {
        EvidenceRepository repository = mock(EvidenceRepository.class);
        ResourceIdGenerator ids = mock(ResourceIdGenerator.class);
        EvidenceContentHasher hasher = new EvidenceContentHasher();
        Payment payment = mock(Payment.class);
        Evidence existing = mock(Evidence.class);
        UUID paymentId = UUID.randomUUID();
        String hash = hasher.sha256("duplicate sms");

        when(payment.getId()).thenReturn(paymentId);
        when(repository.findByPaymentIdAndOriginAndKindAndContentSha256(
            paymentId,
            EvidenceOrigin.PROVIDER,
            EvidenceKind.SMS,
            hash
        )).thenReturn(Optional.of(existing));

        EvidenceService service = new EvidenceService(repository, hasher, ids);
        Evidence result = service.registerRaw(
            payment,
            EvidenceOrigin.PROVIDER,
            EvidenceKind.SMS,
            EvidenceIngestSource.VEROX_BRIDGE,
            "MPESA",
            "duplicate sms",
            null,
            null
        );

        assertThat(result).isSameAs(existing);
        verify(repository, never()).save(any(Evidence.class));
        verify(ids, never()).generate(any());
    }
}
