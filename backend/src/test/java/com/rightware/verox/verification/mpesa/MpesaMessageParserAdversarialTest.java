package com.rightware.verox.verification.mpesa;

import com.rightware.verox.evidence.domain.EvidenceOrigin;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MpesaMessageParserAdversarialTest {

    private final MpesaMessageParser parser = new MpesaMessageParser();

    @Test
    void rejectsCustomerPrefixInjection() {
        assertRejected(
            EvidenceOrigin.CUSTOMER,
            "NOTICE Confirmado DH10L1OJRUS. Transferiste 1.00MT via M-Pesa."
        );
    }

    @Test
    void rejectsCustomerAmountNotBoundToTransferPhrase() {
        assertRejected(
            EvidenceOrigin.CUSTOMER,
            "Confirmado DH10L1OJRUS. Saldo 1.00MT. Transferiste fundos via M-Pesa."
        );
    }

    @Test
    void rejectsProviderAmountNotBoundToReceivedPhrase() {
        assertRejected(
            EvidenceOrigin.PROVIDER,
            "DH10L1OJRUS Confirmed.Fee 1.00MT. You have received funds via M-Pesa."
        );
    }

    @Test
    void rejectsTwoCustomerTransactionClaims() {
        assertRejected(
            EvidenceOrigin.CUSTOMER,
            "Confirmado DH10L1OJRUS. Transferiste 1.00MT via M-Pesa. "
                + "Confirmado AB12CD34EF56. Transferiste 1.00MT via M-Pesa."
        );
    }

    @Test
    void rejectsTwoProviderReceiptClaims() {
        assertRejected(
            EvidenceOrigin.PROVIDER,
            "DH10L1OJRUS Confirmed.You have received 1.00MT via M-Pesa. "
                + "AB12CD34EF56 Confirmed.You have received 1.00MT via M-Pesa."
        );
    }

    @Test
    void rejectsDuplicateCustomerTransferAmounts() {
        assertRejected(
            EvidenceOrigin.CUSTOMER,
            "Confirmado DH10L1OJRUS. Transferiste 1.00MT via M-Pesa. "
                + "Transferiste 2.00MT via M-Pesa."
        );
    }

    @Test
    void rejectsZeroWidthUnicodeFormatCharacter() {
        assertRejected(
            EvidenceOrigin.CUSTOMER,
            "Confirmado DH10L1OJRUS. Transferiste 1.00MT via M-\u200BPesa."
        );
    }

    @Test
    void rejectsBidirectionalOverrideCharacter() {
        assertRejected(
            EvidenceOrigin.PROVIDER,
            "DH10L1OJRUS Confirmed.You have received 1.00MT via M-Pesa.\u202E"
        );
    }

    @Test
    void rejectsUnexpectedControlCharacter() {
        assertRejected(
            EvidenceOrigin.CUSTOMER,
            "Confirmado DH10L1OJRUS. Transferiste 1.00MT via M-Pesa.\u0000"
        );
    }

    @Test
    void rejectsMalformedThreeDecimalCustomerAmount() {
        assertRejected(
            EvidenceOrigin.CUSTOMER,
            "Confirmado DH10L1OJRUS. Transferiste 1.000MT via M-Pesa."
        );
    }

    @Test
    void allowsAdditionalNonTransactionAmountsAfterCustomerAmount() {
        ParsedMpesaMessage parsed = parser.parse(
            EvidenceOrigin.CUSTOMER,
            "Confirmado DH10L1OJRUS. Transferiste 1.00MT via M-Pesa. "
                + "Taxa 0.50MT. Saldo 99.00MT."
        );

        assertThat(parsed.isMatchReady()).isTrue();
        assertThat(parsed.transactionReference()).isEqualTo("DH10L1OJRUS");
        assertThat(parsed.amountMinor()).isEqualTo(100L);
    }

    @Test
    void allowsAdditionalNonTransactionAmountsAfterProviderAmount() {
        ParsedMpesaMessage parsed = parser.parse(
            EvidenceOrigin.PROVIDER,
            "DH10L1OJRUS Confirmed.You have received 1.00MT via M-Pesa. "
                + "Balance 99.00MT."
        );

        assertThat(parsed.isMatchReady()).isTrue();
        assertThat(parsed.transactionReference()).isEqualTo("DH10L1OJRUS");
        assertThat(parsed.amountMinor()).isEqualTo(100L);
    }

    @Test
    void allowsOrdinaryLineBreakWhitespace() {
        ParsedMpesaMessage parsed = parser.parse(
            EvidenceOrigin.CUSTOMER,
            "Confirmado DH10L1OJRUS.\r\nTransferiste 1.00MT via M-Pesa."
        );

        assertThat(parsed.isMatchReady()).isTrue();
        assertThat(parsed.amountMinor()).isEqualTo(100L);
    }

    private void assertRejected(EvidenceOrigin origin, String content) {
        ParsedMpesaMessage parsed = parser.parse(origin, content);

        assertThat(parsed.recognizedFormat()).isFalse();
        assertThat(parsed.isMatchReady()).isFalse();
    }
}