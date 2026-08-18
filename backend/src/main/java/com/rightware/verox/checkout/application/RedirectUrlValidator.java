package com.rightware.verox.checkout.application;

import com.rightware.verox.common.web.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

@Component
public class RedirectUrlValidator {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private final boolean production;
    private final boolean allowHttpLocalDevelopment;

    public RedirectUrlValidator(
        Environment environment,
        @Value("${verox.checkout.redirect.allow-http-local-development:false}")
        boolean allowHttpLocalDevelopment
    ) {
        this.production = environment.acceptsProfiles(Profiles.of("production"));
        this.allowHttpLocalDevelopment =
            !production && allowHttpLocalDevelopment;
    }

    public String validate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw invalid(fieldName, fieldName + " is required.");
        }

        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();

            if (scheme == null || uri.getHost() == null) {
                throw invalid(
                    fieldName,
                    fieldName + " must be an absolute http or https URL."
                );
            }

            scheme = scheme.toLowerCase(Locale.ROOT);

            if (!ALLOWED_SCHEMES.contains(scheme)) {
                throw invalid(
                    fieldName,
                    fieldName + " must be an absolute http or https URL."
                );
            }

            if (production && !scheme.equals("https")) {
                throw invalid(
                    fieldName,
                    fieldName + " must use HTTPS in production."
                );
            }

            if (!production
                && !allowHttpLocalDevelopment
                && !scheme.equals("https")) {
                throw invalid(
                    fieldName,
                    fieldName
                        + " HTTP URLs require the explicit local-development exception."
                );
            }

            return uri.toString();
        } catch (ApiException exception) {
            throw exception;
        } catch (IllegalArgumentException exception) {
            throw invalid(
                fieldName,
                fieldName + " must be a valid absolute URL."
            );
        }
    }

    private ApiException invalid(String fieldName, String message) {
        return new ApiException(
            HttpStatus.BAD_REQUEST,
            "INVALID_REDIRECT_URL",
            message
        );
    }
}