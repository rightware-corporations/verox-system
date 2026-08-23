package com.rightware.verox.authentication.application;

import com.rightware.verox.authentication.domain.MerchantOperator;
import com.rightware.verox.authentication.domain.MerchantOperatorSession;
import com.rightware.verox.authentication.repository.MerchantOperatorRepository;
import com.rightware.verox.authentication.repository.MerchantOperatorSessionRepository;
import com.rightware.verox.common.web.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

@Service
public class MerchantOperatorSessionService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final MerchantOperatorRepository operatorRepository;
    private final MerchantOperatorSessionRepository sessionRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Duration sessionTtl;

    public MerchantOperatorSessionService(
        MerchantOperatorRepository operatorRepository,
        MerchantOperatorSessionRepository sessionRepository,
        @Value("${verox.operator-session.ttl-hours:12}") long ttlHours
    ) {
        if (ttlHours < 1 || ttlHours > 168) {
            throw new IllegalArgumentException("operator session ttl-hours must be between 1 and 168");
        }
        this.operatorRepository = operatorRepository;
        this.sessionRepository = sessionRepository;
        this.sessionTtl = Duration.ofHours(ttlHours);
    }

    @Transactional
    public LoginResult login(String username, String password) {
        String normalized = normalizeUsername(username);
        MerchantOperator operator = operatorRepository.findByUsername(normalized).orElse(null);
        if (operator == null || !operator.isActive() || !operator.getMerchant().isActive()
            || password == null || !passwordEncoder.matches(password, operator.getPasswordHash())) {
            throw invalidCredentials();
        }

        String sessionToken = randomToken("vx_ops_");
        String csrfToken = randomToken("vx_csrf_");
        Instant expiresAt = Instant.now().plus(sessionTtl);
        MerchantOperatorSession session = new MerchantOperatorSession(
            operator,
            sha256(sessionToken),
            sha256(csrfToken),
            expiresAt
        );
        sessionRepository.save(session);
        return new LoginResult(toPrincipal(session), sessionToken, csrfToken, expiresAt);
    }

    @Transactional
    public Optional<AuthenticatedSession> authenticate(String rawSessionToken) {
        if (rawSessionToken == null || rawSessionToken.isBlank()) return Optional.empty();
        MerchantOperatorSession session = sessionRepository.findByTokenHash(sha256(rawSessionToken.trim())).orElse(null);
        Instant now = Instant.now();
        if (session == null || !session.isActiveAt(now) || !session.getOperator().isActive()
            || !session.getOperator().getMerchant().isActive()) {
            return Optional.empty();
        }
        session.touch(now);
        return Optional.of(new AuthenticatedSession(toPrincipal(session), session.getCsrfTokenHash(), session.getExpiresAt()));
    }

    @Transactional
    public void logout(String rawSessionToken) {
        if (rawSessionToken == null || rawSessionToken.isBlank()) return;
        sessionRepository.findByTokenHash(sha256(rawSessionToken.trim()))
            .ifPresent(session -> session.revoke(Instant.now()));
    }

    public boolean csrfMatches(String expectedHash, String candidate) {
        if (expectedHash == null || candidate == null || candidate.isBlank()) return false;
        byte[] expected = expectedHash.getBytes(StandardCharsets.UTF_8);
        byte[] actual = sha256(candidate.trim()).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    private MerchantOperatorPrincipal toPrincipal(MerchantOperatorSession session) {
        MerchantOperator operator = session.getOperator();
        return new MerchantOperatorPrincipal(
            operator.getId(),
            operator.getDisplayName(),
            operator.getMerchant().getId(),
            operator.getMerchant().getName(),
            operator.getEnvironment(),
            session.getId()
        );
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) throw invalidCredentials();
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private String randomToken(String prefix) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return prefix + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash operator session token", exception);
        }
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_OPERATOR_CREDENTIALS", "Invalid credentials.");
    }

    public record LoginResult(MerchantOperatorPrincipal principal, String sessionToken, String csrfToken, Instant expiresAt) {}
    public record AuthenticatedSession(MerchantOperatorPrincipal principal, String csrfTokenHash, Instant expiresAt) {}
}
