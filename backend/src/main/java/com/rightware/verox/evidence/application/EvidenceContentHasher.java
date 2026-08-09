package com.rightware.verox.evidence.application;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class EvidenceContentHasher {

    public String sha256(byte[] content) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("Evidence content is required");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public String sha256(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Evidence content is required");
        }
        return sha256(content.getBytes(StandardCharsets.UTF_8));
    }
}
