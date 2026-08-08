package com.rightware.verox.checkout.application;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class CheckoutRequestFingerprint {

    public String create(
        long amountMinor,
        String currency,
        String externalReference,
        String description,
        String successUrl,
        String cancelUrl
    ) {
        String canonical = String.join(
            "\u001f",
            Long.toString(amountMinor),
            currency,
            externalReference,
            description == null ? "" : description,
            successUrl,
            cancelUrl
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
