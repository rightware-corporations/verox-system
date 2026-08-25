package com.rightware.verox;

import com.rightware.verox.checkout.application.CheckoutSubmissionCapabilityService;
import com.rightware.verox.checkout.application.HostedCheckoutBootstrapService;
import com.rightware.verox.checkout.repository.CheckoutSessionRepository;
import com.rightware.verox.common.money.MoneyConverter;
import com.rightware.verox.evidence.repository.EvidenceRepository;
import com.rightware.verox.payment.application.PaymentService;
import com.rightware.verox.payment.repository.PaymentRepository;
import com.rightware.verox.paymentchannel.application.PaymentChannelService;
import com.rightware.verox.pilot.repository.PilotManualPaymentAcceptanceRepository;
import com.rightware.verox.pilot.repository.PilotManualPaymentRejectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class StartupConstructorInjectionTest {
    @Test
    void auditedMultiConstructorServicesLoadThroughProductionConstructors() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(CheckoutSessionRepository.class, () -> mock(CheckoutSessionRepository.class));
            context.registerBean(PaymentRepository.class, () -> mock(PaymentRepository.class));
            context.registerBean(PilotManualPaymentAcceptanceRepository.class, () -> mock(PilotManualPaymentAcceptanceRepository.class));
            context.registerBean(PilotManualPaymentRejectionRepository.class, () -> mock(PilotManualPaymentRejectionRepository.class));
            context.registerBean(EvidenceRepository.class, () -> mock(EvidenceRepository.class));
            context.registerBean(CheckoutSubmissionCapabilityService.class, () -> mock(CheckoutSubmissionCapabilityService.class));
            context.registerBean(MoneyConverter.class);
            context.registerBean(PaymentChannelService.class, () -> mock(PaymentChannelService.class));
            context.registerBean(HostedCheckoutBootstrapService.class);
            context.registerBean(PaymentService.class);
            context.refresh();

            assertThat(context.getBean(HostedCheckoutBootstrapService.class)).isNotNull();
            assertThat(context.getBean(PaymentService.class)).isNotNull();
        }
    }
}
