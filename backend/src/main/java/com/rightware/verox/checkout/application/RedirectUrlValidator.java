package com.rightware.verox.checkout.application;

import com.rightware.verox.common.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Set;

@Component
public class RedirectUrlValidator {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    public String validate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REDIRECT_URL", fieldName + " is required.");
        }

        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase()) || uri.getHost() == null) {
                throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_REDIRECT_URL",
                    fieldName + " must be an absolute http or https URL."
                );
            }
            return uri.toString();
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "INVALID_REDIRECT_URL",
                fieldName + " must be a valid absolute URL."
            );
        }
    }
}
