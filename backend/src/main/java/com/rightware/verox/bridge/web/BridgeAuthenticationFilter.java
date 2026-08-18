package com.rightware.verox.bridge.web;

import com.rightware.verox.bridge.application.BridgePrincipal;
import com.rightware.verox.bridge.application.BridgeService;
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
public class BridgeAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String BRIDGE_PATH_PREFIX = "/v1/bridges/";

    private final BridgeService bridgeService;
    private final RateLimitGuard rateLimitGuard;

    public BridgeAuthenticationFilter(
        BridgeService bridgeService,
        RateLimitGuard rateLimitGuard
    ) {
        this.bridgeService = bridgeService;
        this.rateLimitGuard = rateLimitGuard;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(BRIDGE_PATH_PREFIX);
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
                "INVALID_BRIDGE_AUTHORIZATION",
                "Bridge Authorization must use Bearer authentication."
            );
            return;
        }

        String rawCredential =
            authorization
                .substring(BEARER_PREFIX.length())
                .trim();

        Optional<BridgePrincipal> principal =
            bridgeService.authenticate(rawCredential);

        if (principal.isEmpty()) {
            writeUnauthorized(
                response,
                "INVALID_BRIDGE_CREDENTIAL",
                "Bridge credential is invalid or inactive."
            );
            return;
        }

        /*
         * Only an authenticated Bridge consumes the legitimate
         * Bridge bucket.
         */
        try {
            rateLimitGuard.checkBridge(
                principal.get().bridgeId()
            );
        } catch (RateLimitExceededException exception) {
            writeRateLimited(response, exception);
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                principal.get(),
                null,
                List.of(
                    new SimpleGrantedAuthority("ROLE_BRIDGE")
                )
            );

        SecurityContext context =
            SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void writeUnauthorized(
        HttpServletResponse response,
        String code,
        String message
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        writeJsonError(response, code, message);
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