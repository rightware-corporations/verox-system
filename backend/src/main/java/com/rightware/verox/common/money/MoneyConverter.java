package com.rightware.verox.common.money;

import com.rightware.verox.common.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

@Component
public class MoneyConverter {

    private static final String MVP_CURRENCY = "MZN";
    private static final int MVP_SCALE = 2;

    public MoneyValue normalize(BigDecimal amount, String currency) {
        if (amount == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "Amount is required.");
        }

        String normalizedCurrency = currency == null ? "" : currency.trim().toUpperCase(Locale.ROOT);
        if (!MVP_CURRENCY.equals(normalizedCurrency)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_CURRENCY", "VEROX MVP currently supports MZN only.");
        }

        try {
            BigDecimal normalizedAmount = amount.setScale(MVP_SCALE, RoundingMode.UNNECESSARY);
            if (normalizedAmount.signum() <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "Amount must be greater than zero.");
            }

            long minor = normalizedAmount.movePointRight(MVP_SCALE).longValueExact();
            return new MoneyValue(minor, normalizedAmount.toPlainString(), normalizedCurrency);
        } catch (ArithmeticException exception) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "INVALID_AMOUNT",
                "Amount must have at most two decimal places and fit within the supported range."
            );
        }
    }

    public String toMajorString(long amountMinor) {
        return BigDecimal.valueOf(amountMinor, MVP_SCALE).toPlainString();
    }

    public record MoneyValue(long minor, String major, String currency) {
    }
}
