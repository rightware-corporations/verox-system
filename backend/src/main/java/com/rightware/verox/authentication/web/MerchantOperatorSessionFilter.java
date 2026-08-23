package com.rightware.verox.authentication.web;

import com.rightware.verox.authentication.application.MerchantOperatorSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

@Component
public class MerchantOperatorSessionFilter extends OncePerRequestFilter {
    public static final String SESSION_COOKIE = "VEROX_OPERATOR_SESSION";
    public static final String CSRF_HEADER = "X-VEROX-CSRF";
    private static final String PLATFORM_PREFIX = "/platform/v1/";
    private static final String LOGIN_PATH = "/platform/v1/auth/login";

    private final MerchantOperatorSessionService sessionService;

    public MerchantOperatorSessionFilter(MerchantOperatorSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !uri.startsWith(PLATFORM_PREFIX) || LOGIN_PATH.equals(uri);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String token = cookieValue(request, SESSION_COOKIE);
        var authenticated = sessionService.authenticate(token);
        if (authenticated.isEmpty()) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "OPERATOR_SESSION_REQUIRED", "A valid operator session is required.");
            return;
        }

        if (requiresCsrf(request.getMethod())
            && !sessionService.csrfMatches(authenticated.get().csrfTokenHash(), request.getHeader(CSRF_HEADER))) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "INVALID_CSRF_TOKEN", "A valid CSRF token is required.");
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            authenticated.get().principal(), null, List.of(new SimpleGrantedAuthority("ROLE_OPERATOR"))
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean requiresCsrf(String method) {
        return !("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method));
    }

    private String cookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) if (name.equals(cookie.getName())) return cookie.getValue();
        return null;
    }

    private void writeError(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}}");
    }
}
