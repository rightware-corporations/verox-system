package com.rightware.verox.verification.mpesa;

import com.rightware.verox.evidence.domain.EvidenceOrigin;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MpesaMessageParserTest {

    private final MpesaMessageParser parser = new MpesaMessageParser();

    @Test
    void parsesCustomerConfirmationMessage() {
        ParsedMpesaMessage parsed = parser.parse(
            EvidenceOrigin.CUSTOMER,
            "Confirmado DH10L1OJRUS. Transferiste 1.00MT via M-Pesa."
        );

        assertThat(parsed.origin()).isEqualTo(EvidenceOrigin.CUSTOMER);
        assertThat(parsed.transactionReference()).isEqualTo("DH10L1OJRUS");
        assertThat(parsed.amountMinor()).isEqualTo(100L);
        assertThat(parsed.currency()).isEqualTo("MZN");
        assertThat(parsed.recognizedFormat()).isTrue();
        assertThat(parsed.isMatchReady()).isTrue();
    }

    @Test
    void parsesProviderReceivingMessage() {
        ParsedMpesaMessage parsed = parser.parse(
            EvidenceOrigin.PROVIDER,
            "DH10L1OJRUS Confirmed.You have received 1.00MT via M-Pesa."
        );

        assertThat(parsed.origin()).isEqualTo(EvidenceOrigin.PROVIDER);
        assertThat(parsed.transactionReference()).isEqualTo("DH10L1OJRUS");
        assertThat(parsed.amountMinor()).isEqualTo(100L);
        assertThat(parsed.currency()).isEqualTo("MZN");
        assertThat(parsed.recognizedFormat()).isTrue();
        assertThat(parsed.isMatchReady()).isTrue();
    }

    @Test
    void customerAndProviderSamplesNormalizeToSameReferenceAndAmount() {
        ParsedMpesaMessage customer = parser.parse(
            EvidenceOrigin.CUSTOMER,
            "Confirmado DH10L1OJRUS. Transferiste 1.00MT via M-Pesa."
        );
        ParsedMpesaMessage provider = parser.parse(
            EvidenceOrigin.PROVIDER,
            "DH10L1OJRUS Confirmed.You have received 1.00MT via M-Pesa."
        );

        assertThat(customer.transactionReference()).isEqualTo(provider.transactionReference());
        assertThat(customer.amountMinor()).isEqualTo(provider.amountMinor());
        assertThat(customer.currency()).isEqualTo(provider.currency());
    }

    @Test
    void doesNotTreatUnknownTextAsMatchReady() {
        ParsedMpesaMessage parsed = parser.parse(
            EvidenceOrigin.CUSTOMER,
            "Pagamento efetuado com sucesso. Obrigado."
        );

        assertThat(parsed.transactionReference()).isNull();
        assertThat(parsed.amountMinor()).isNull();
        assertThat(parsed.currency()).isNull();
        assertThat(parsed.recognizedFormat()).isFalse();
        assertThat(parsed.isMatchReady()).isFalse();
    }

    @Test
    void keepsCustomerAndProviderReferenceRulesSeparate() {
        ParsedMpesaMessage wrongCustomerShape = parser.parse(
            EvidenceOrigin.CUSTOMER,
            "DH10L1OJRUS Confirmed.You have received 1.00MT via M-Pesa."
        );

        assertThat(wrongCustomerShape.isMatchReady()).isFalse();
    }
}
