package com.rightware.verox.verification.mpesa;

import com.rightware.verox.evidence.domain.EvidenceOrigin;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MpesaMessageParser {

    private static final String REFERENCE = "[A-Z0-9]{8,20}";
    private static final String AMOUNT = "\\d+(?:[.,]\\d{1,2})?";

    /*
     * Known MVP Customer template:
     *
     * Confirmado <reference>. Transferiste <amount>MT ...
     *
     * The transaction amount must be structurally bound to "Transferiste".
     * Additional fee/balance amounts may follow later in the SMS.
     */
    private static final Pattern CUSTOMER_TEMPLATE = Pattern.compile(
        "(?i)^\\s*(?:confirmado|confirmed)\\s+(" + REFERENCE + ")\\.\\s*"
            + "transferiste\\s+(" + AMOUNT + ")\\s*MT\\b"
    );

    /*
     * Known MVP Provider template:
     *
     * <reference> Confirmed.You have received <amount>MT ...
     *
     * Additional balance/fee amounts may follow later in the SMS.
     */
    private static final Pattern PROVIDER_TEMPLATE = Pattern.compile(
        "(?i)^\\s*(" + REFERENCE + ")\\s+(?:confirmed|confirmado)\\.\\s*"
            + "you\\s+have\\s+received\\s+(" + AMOUNT + ")\\s*MT\\b"
    );

    private static final Pattern CUSTOMER_REFERENCE_CLAIM = Pattern.compile(
        "(?i)\\b(?:confirmado|confirmed)\\s+" + REFERENCE + "\\b"
    );

    private static final Pattern CUSTOMER_TRANSFER_AMOUNT = Pattern.compile(
        "(?i)\\btransferiste\\s+" + AMOUNT + "\\s*MT\\b"
    );

    private static final Pattern PROVIDER_REFERENCE_CLAIM = Pattern.compile(
        "(?i)\\b" + REFERENCE + "\\s+(?:confirmed|confirmado)\\b"
    );

    private static final Pattern PROVIDER_RECEIVED_AMOUNT = Pattern.compile(
        "(?i)\\byou\\s+have\\s+received\\s+" + AMOUNT + "\\s*MT\\b"
    );

    public ParsedMpesaMessage parse(EvidenceOrigin origin, String rawContent) {
        Objects.requireNonNull(origin, "origin");

        if (rawContent == null || rawContent.isBlank()) {
            return unrecognized(origin);
        }

        if (containsUnsafeCharacters(rawContent)) {
            return unrecognized(origin);
        }

        String content = normalizeWhitespace(rawContent);

        return switch (origin) {
            case CUSTOMER -> parseKnownTemplate(
                origin,
                content,
                CUSTOMER_TEMPLATE,
                CUSTOMER_REFERENCE_CLAIM,
                CUSTOMER_TRANSFER_AMOUNT
            );
            case PROVIDER -> parseKnownTemplate(
                origin,
                content,
                PROVIDER_TEMPLATE,
                PROVIDER_REFERENCE_CLAIM,
                PROVIDER_RECEIVED_AMOUNT
            );
        };
    }

    private ParsedMpesaMessage parseKnownTemplate(
        EvidenceOrigin origin,
        String content,
        Pattern template,
        Pattern referenceClaim,
        Pattern transactionAmountClaim
    ) {
        /*
         * Multiple transaction references or multiple transaction-bound
         * amounts are ambiguous. VEROX must never guess which one is real.
         */
        if (countMatches(referenceClaim, content) != 1
            || countMatches(transactionAmountClaim, content) != 1) {
            return unrecognized(origin);
        }

        Matcher matcher = template.matcher(content);
        if (!matcher.find()) {
            return unrecognized(origin);
        }

        String reference = matcher.group(1).toUpperCase(Locale.ROOT);
        Long amountMinor = parseAmountMinor(matcher.group(2));

        if (amountMinor == null) {
            return unrecognized(origin);
        }

        return new ParsedMpesaMessage(
            origin,
            reference,
            amountMinor,
            "MZN",
            true
        );
    }

    private int countMatches(Pattern pattern, String content) {
        Matcher matcher = pattern.matcher(content);
        int count = 0;

        while (matcher.find()) {
            count++;
            if (count > 1) {
                return count;
            }
        }

        return count;
    }

    private Long parseAmountMinor(String rawAmount) {
        String normalized = rawAmount.replace(',', '.');

        try {
            BigDecimal amount = new BigDecimal(normalized)
                .setScale(2, RoundingMode.UNNECESSARY);

            if (amount.signum() < 0) {
                return null;
            }

            return amount.movePointRight(2).longValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            return null;
        }
    }

    private boolean containsUnsafeCharacters(String content) {
        return content.codePoints().anyMatch(codePoint -> {
            if (codePoint == '\r' || codePoint == '\n' || codePoint == '\t') {
                return false;
            }

            return Character.isISOControl(codePoint)
                || Character.getType(codePoint) == Character.FORMAT;
        });
    }

    private String normalizeWhitespace(String rawContent) {
        return rawContent.trim().replaceAll("\\s+", " ");
    }

    private ParsedMpesaMessage unrecognized(EvidenceOrigin origin) {
        return new ParsedMpesaMessage(
            origin,
            null,
            null,
            null,
            false
        );
    }
}