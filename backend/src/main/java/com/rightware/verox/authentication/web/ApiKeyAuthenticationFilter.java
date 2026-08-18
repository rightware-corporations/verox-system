package com.rightware.verox.authentication.web;

import com.rightware.verox.authentication.application.ApiKeyService;
import com.rightware.verox.authentication.application.MerchantPrincipal;
import com.rightware.verox.common.ratelimit.RateLimitExceededException;
import com.rightware.verox.common.ratelimit.RateLimitGuard;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String API_PATH_PREFIX = "/v1/";
    private static final String BRIDGE_PATH_PREFIX = "/v1/bridges/";

    private final ApiKeyService apiKeyService;
    private final RateLimitGuard rateLimitGuard;

    public ApiKeyAuthenticationFilter(
        ApiKeyService apiKeyService,
        RateLimitGuard rateLimitGuard
    ) {
        this.apiKeyService = apiKeyService;
        this.rateLimitGuard = rateLimitGuard;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();

        return !uri.startsWith(API_PATH_PREFIX)
            || uri.startsWith(BRIDGE_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String authorization =
            request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization == null || authorization.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!authorization.startsWith(BEARER_PREFIX)) {
            writeUnauthorized(
                response,
                "INVALID_AUTHORIZATION_HEADER",
                "Authorization must use Bearer authentication."
            );
            return;
        }

        String rawApiKey =
            authorization
                .substring(BEARER_PREFIX.length())
                .trim();

        if (rawApiKey.isBlank()) {
            writeUnauthorized(
                response,
                "INVALID_API_KEY",
                "API key is missing."
            );
            return;
        }

        Optional<MerchantPrincipal> principal =
            apiKeyService.authenticate(rawApiKey);

        if (principal.isEmpty()) {
            writeUnauthorized(
                response,
                "INVALID_API_KEY",
                "API key is invalid or inactive."
            );
            return;
        }

        /*
         * Only a successfully authenticated credential consumes
         * the legitimate Merchant API quota.
         */
        try {
            rateLimitGuard.checkMerchantApi(
                principal.get().apiKeyId()
            );
        } catch (RateLimitExceededException exception) {
            writeRateLimited(
                response,
                exception
            );
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                principal.get(),
                null,
                List.of(
                    new SimpleGrantedAuthority(
                        "ROLE_MERCHANT"
                    )
                )
            );

        SecurityContext context =
            SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        try {
            filterChain.doFilter(
                request,
                response
            );
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void writeUnauthorized(
        HttpServletResponse response,
        String code,
        String message
    ) throws IOException {
        response.setStatus(
            HttpServletResponse.SC_UNAUTHORIZED
        );

        writeJsonError(
            response,
            code,
            message
        );
    }

    private void writeRateLimited(
        HttpServletResponse response,
        RateLimitExceededException exception
    ) throws IOException {
        response.setStatus(
            HttpStatus.TOO_MANY_REQUESTS.value()
        );

        response.setHeader(
            HttpHeaders.RETRY_AFTER,
            Long.toString(
                exception.getRetryAfterSeconds()
            )
        );

        writeJsonError(
            response,
            exception.getCode(),
            exception.getMessage()
        );
    }

    private void writeJsonError(
        HttpServletResponse response,
        String code,
        String message
    ) throws IOException {
        response.setCharacterEncoding(
            StandardCharsets.UTF_8.name()
        );

        response.setContentType(
            MediaType.APPLICATION_JSON_VALUE
        );

        response.getWriter().write(
            "{\"error\":{\"code\":\""
                + code
                + "\",\"message\":\""
                + message
                + "\"}}"
        );
    }
}