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
    void registersUnlinkedRawProviderEvidenceFromVeroxBridgeInExplicitEnvironment() {
        EvidenceRepository repository = mock(EvidenceRepository.class);
        ResourceIdGenerator ids = mock(ResourceIdGenerator.class);
        EvidenceContentHasher hasher = new EvidenceContentHasher();
        Merchant merchant = new Merchant("Event Merchant");
        String hash = hasher.sha256("M-Pesa official SMS");

        when(ids.generate("ev")).thenReturn("ev_test123");
        when(repository.findByMerchantIdAndEnvironmentAndOriginAndKindAndContentSha256(
            merchant.getId(),
            ApiKeyEnvironment.LIVE,
            EvidenceOrigin.PROVIDER,
            EvidenceKind.SMS,
            hash
        )).thenReturn(Optional.empty());
        when(repository.save(any(Evidence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EvidenceService service = new EvidenceService(repository, hasher, ids);
        Evidence evidence = service.registerProviderRaw(
            merchant,
            ApiKeyEnvironment.LIVE,
            EvidenceKind.SMS,
            EvidenceIngestSource.VEROX_BRIDGE,
            "MPESA",
            "M-Pesa official SMS",
            Instant.parse("2026-08-09T12:00:00Z"),
            Instant.parse("2026-08-09T12:00:02Z")
        );

        assertThat(evidence.getPublicId()).isEqualTo("ev_test123");
        assertThat(evidence.getEnvironment()).isEqualTo(ApiKeyEnvironment.LIVE);
        assertThat(evidence.getOrigin()).isEqualTo(EvidenceOrigin.PROVIDER);
        assertThat(evidence.getKind()).isEqualTo(EvidenceKind.SMS);
        assertThat(evidence.getIngestSource()).isEqualTo(EvidenceIngestSource.VEROX_BRIDGE);
        assertThat(evidence.getProvider()).isEqualTo("MPESA");
        assertThat(evidence.getRawContent()).isEqualTo("M-Pesa official SMS");
        assertThat(evidence.getContentSha256()).isEqualTo(hash);
        assertThat(evidence.getPayment()).isNull();
        verify(repository).save(any(Evidence.class));
    }

    @Test
    void returnsExistingProviderEvidenceOnlyInsideSameEnvironment() {
        EvidenceRepository repository = mock(EvidenceRepository.class);
        ResourceIdGenerator ids = mock(ResourceIdGenerator.class);
        EvidenceContentHasher hasher = new EvidenceContentHasher();
        Merchant merchant = new Merchant("Event Merchant");
        Evidence existing = mock(Evidence.class);
        String hash = hasher.sha256("duplicate sms");

        when(repository.findByMerchantIdAndEnvironmentAndOriginAndKindAndContentSha256(
            merchant.getId(),
            ApiKeyEnvironment.TEST,
            EvidenceOrigin.PROVIDER,
            EvidenceKind.SMS,
            hash
        )).thenReturn(Optional.of(existing));

        EvidenceService service = new EvidenceService(repository, hasher, ids);
        Evidence result = service.registerProviderRaw(
            merchant,
            ApiKeyEnvironment.TEST,
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

    @Test
    void registersCustomerStoredEvidenceLinkedToPaymentEnvironment() {
        EvidenceRepository repository = mock(EvidenceRepository.class);
        ResourceIdGenerator ids = mock(ResourceIdGenerator.class);
        EvidenceContentHasher hasher = new EvidenceContentHasher();
        Merchant merchant = new Merchant("Event Merchant");
        Payment payment = mock(Payment.class);
        UUID paymentId = UUID.randomUUID();
        String hash = hasher.sha256("image bytes");

        when(payment.getId()).thenReturn(paymentId);
        when(payment.getMerchant()).thenReturn(merchant);
        when(payment.getEnvironment()).thenReturn(ApiKeyEnvironment.LIVE);
        when(ids.generate("ev")).thenReturn("ev_customer123");
        when(repository.findByPaymentIdAndOriginAndKindAndContentSha256(
            paymentId,
            EvidenceOrigin.CUSTOMER,
            EvidenceKind.IMAGE,
            hash
        )).thenReturn(Optional.empty());
        when(repository.save(any(Evidence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EvidenceService service = new EvidenceService(repository, hasher, ids);
        Evidence evidence = service.registerCustomerStored(
            payment,
            EvidenceKind.IMAGE,
            EvidenceIngestSource.HOSTED_CHECKOUT,
            "MPESA",
            hash,
            "image/jpeg",
            "proof.jpg",
            "evidence/customer/proof.jpg",
            null,
            Instant.parse("2026-08-09T12:05:00Z")
        );

        assertThat(evidence.getPublicId()).isEqualTo("ev_customer123");
        assertThat(evidence.getEnvironment()).isEqualTo(ApiKeyEnvironment.LIVE);
        assertThat(evidence.getOrigin()).isEqualTo(EvidenceOrigin.CUSTOMER);
        assertThat(evidence.getPayment()).isSameAs(payment);
        assertThat(evidence.getStorageKey()).isEqualTo("evidence/customer/proof.jpg");
        assertThat(evidence.getRawContent()).isNull();
    }
}
