package com.rightware.verox.authentication.web;

import com.rightware.verox.authentication.application.MerchantOperatorPrincipal;
import com.rightware.verox.authentication.application.MerchantOperatorSessionService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Set;

@RestController
@RequestMapping("/platform/v1/auth")
public class MerchantOperatorAuthController {
    private static final String CSRF_COOKIE = "VEROX_CSRF";
    private static final Set<String> ALLOWED_SAME_SITE = Set.of("Strict", "Lax", "None");
    private final MerchantOperatorSessionService sessionService;
    private final boolean secureCookie;
    private final String sameSite;

    public MerchantOperatorAuthController(
        MerchantOperatorSessionService sessionService,
        @Value("${verox.operator-session.cookie-secure:true}") boolean secureCookie,
        @Value("${verox.operator-session.cookie-same-site:None}") String sameSite
    ) {
        if (!ALLOWED_SAME_SITE.contains(sameSite)) {
            throw new IllegalArgumentException("operator session cookie same-site must be Strict, Lax or None");
        }
        if ("None".equals(sameSite) && !secureCookie) {
            throw new IllegalArgumentException("SameSite=None requires secure operator session cookies");
        }
        this.sessionService = sessionService;
        this.secureCookie = secureCookie;
        this.sameSite = sameSite;
    }

    @PostMapping("/login")
    public SessionResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        var result = sessionService.login(request.username(), request.password());
        Duration maxAge = Duration.between(java.time.Instant.now(), result.expiresAt());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(MerchantOperatorSessionFilter.SESSION_COOKIE, result.sessionToken(), true, maxAge));
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(CSRF_COOKIE, result.csrfToken(), false, maxAge));
        return SessionResponse.from(result.principal());
    }

    @GetMapping("/session")
    public SessionResponse session(@AuthenticationPrincipal MerchantOperatorPrincipal principal) {
        return SessionResponse.from(principal);
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        sessionService.logout(cookieValue(request, MerchantOperatorSessionFilter.SESSION_COOKIE));
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(MerchantOperatorSessionFilter.SESSION_COOKIE, "", true, Duration.ZERO));
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(CSRF_COOKIE, "", false, Duration.ZERO));
    }

    private String cookie(String name, String value, boolean httpOnly, Duration maxAge) {
        return ResponseCookie.from(name, value)
            .httpOnly(httpOnly)
            .secure(secureCookie)
            .sameSite(sameSite)
            .path("/platform/v1")
            .maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge)
            .build().toString();
    }

    private String cookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) if (name.equals(cookie.getName())) return cookie.getValue();
        return null;
    }

    public record LoginRequest(@NotBlank @Size(max = 160) String username, @NotBlank @Size(max = 255) String password) {}

    public record SessionResponse(
        String operatorId,
        String operatorDisplayName,
        String merchantId,
        String merchantName,
        String environment
    ) {
        static SessionResponse from(MerchantOperatorPrincipal principal) {
            return new SessionResponse(
                principal.operatorId().toString(),
                principal.operatorDisplayName(),
                principal.merchantId().toString(),
                principal.merchantName(),
                principal.environment().name()
            );
        }
    }
}
