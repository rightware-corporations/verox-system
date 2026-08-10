package com.rightware.verox.verification.application;

import com.rightware.verox.evidence.application.EvidenceIngestedEvent;
import com.rightware.verox.evidence.domain.EvidenceOrigin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class VerificationEvidenceIngestedListener {

    private static final Logger log = LoggerFactory.getLogger(VerificationEvidenceIngestedListener.class);

    private final VerificationOrchestrator verificationOrchestrator;

    public VerificationEvidenceIngestedListener(VerificationOrchestrator verificationOrchestrator) {
        this.verificationOrchestrator = verificationOrchestrator;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEvidenceIngested(EvidenceIngestedEvent event) {
        try {
            if (event.origin() == EvidenceOrigin.CUSTOMER && event.paymentId() != null) {
                VerificationRunResult result = verificationOrchestrator.verifyPayment(event.paymentId());
                log.info(
                    "VEROX Verification Engine processed customer evidence {} for payment {}: {} / {}",
                    event.evidencePublicId(),
                    result.paymentId(),
                    result.status(),
                    result.reason()
                );
                return;
            }

            verificationOrchestrator.verifyMerchant(event.merchantId());
            log.info(
                "VEROX Verification Engine processed provider evidence {} for merchant {}.",
                event.evidencePublicId(),
                event.merchantId()
            );
        } catch (RuntimeException exception) {
            log.error(
                "VEROX Verification Engine failed after committed evidence {}. Evidence remains persisted for retry.",
                event.evidencePublicId(),
                exception
            );
        }
    }
}
