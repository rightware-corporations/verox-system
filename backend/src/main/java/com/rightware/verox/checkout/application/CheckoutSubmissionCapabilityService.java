package com.rightware.verox.checkout.application;

import com.rightware.verox.checkout.domain.CheckoutSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Service
public class CheckoutSubmissionCapabilityService {

    private static final String TOKEN_PREFIX = "vx_checkout_";
    private static final String PURPOSE = "verox-checkout-submission-v1";

    private final byte[] masterSecret;

    public CheckoutSubmissionCapabilityService(
        @Value("${verox.checkout.capability-master-secret:verox-dev-checkout-capability-master-secret-change-me}") String masterSecret
    ) {
        if (masterSecret == null || masterSecret.isBlank()) {
            throw new IllegalArgumentException("VEROX checkout capability master secret is required");
        }
        this.masterSecret = masterSecret.getBytes(StandardCharsets.UTF_8);
    }

    public String issue(CheckoutSession session) {
        String payload = PURPOSE
            + "|" + session.getMerchant().getId()
            + "|" + session.getEnvironment().name()
            + "|" + session.getPublicId();
        byte[] derived = hmac(masterSecret, payload.getBytes(StandardCharsets.UTF_8));
        return TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(derived);
    }

    public boolean matches(CheckoutSession session, String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return false;
        }
        byte[] expected = issue(session).getBytes(StandardCharsets.UTF_8);
        byte[] actual = candidate.trim().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    private byte[] hmac(byte[] key, byte[] value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to derive checkout submission capability", exception);
        }
    }
}
