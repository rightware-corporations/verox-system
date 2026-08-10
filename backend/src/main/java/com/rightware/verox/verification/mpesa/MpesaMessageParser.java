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

    private static final Pattern CUSTOMER_REFERENCE = Pattern.compile(
        "(?i)\\b(?:confirmado|confirmed)\\s+([A-Z0-9]{8,20})\\b"
    );

    private static final Pattern PROVIDER_REFERENCE = Pattern.compile(
        "(?i)^\\s*([A-Z0-9]{8,20})\\s+(?:confirmed|confirmado)\\b"
    );

    private static final Pattern AMOUNT = Pattern.compile(
        "(?i)\\b(\\d+(?:[.,]\\d{1,2})?)\\s*MT\\b"
    );

    public ParsedMpesaMessage parse(EvidenceOrigin origin, String rawContent) {
        Objects.requireNonNull(origin, "origin");
        String content = normalizeContent(rawContent);

        Pattern referencePattern = switch (origin) {
            case CUSTOMER -> CUSTOMER_REFERENCE;
            case PROVIDER -> PROVIDER_REFERENCE;
        };

        String reference = extractReference(referencePattern, content);
        Long amountMinor = extractAmountMinor(content);
        boolean recognizedFormat = reference != null && amountMinor != null;

        return new ParsedMpesaMessage(
            origin,
            reference,
            amountMinor,
            recognizedFormat ? "MZN" : null,
            recognizedFormat
        );
    }

    private String extractReference(Pattern pattern, String content) {
        Matcher matcher = pattern.matcher(content);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1).toUpperCase(Locale.ROOT);
    }

    private Long extractAmountMinor(String content) {
        Matcher matcher = AMOUNT.matcher(content);
        if (!matcher.find()) {
            return null;
        }

        String normalized = matcher.group(1).replace(',', '.');
        try {
            BigDecimal amount = new BigDecimal(normalized).setScale(2, RoundingMode.UNNECESSARY);
            if (amount.signum() < 0) {
                return null;
            }
            return amount.movePointRight(2).longValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            return null;
        }
    }

    private String normalizeContent(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return "";
        }
        return rawContent.trim().replaceAll("\\s+", " ");
    }
}
