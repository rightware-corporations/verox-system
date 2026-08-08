package com.rightware.verox.common.id;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class ResourceIdGenerator {

    private static final int RANDOM_BYTES = 18;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate(String prefix) {
        byte[] random = new byte[RANDOM_BYTES];
        secureRandom.nextBytes(random);
        return prefix + "_" + Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }
}
